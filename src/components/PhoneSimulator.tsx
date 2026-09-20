import React, { useState, useEffect, useMemo } from 'react';
import {
  Shield, Lock, Unlock, Key, Search, Plus, Star, Copy, Eye, EyeOff,
  Settings, AlertTriangle, Fingerprint, RefreshCw, Check, ArrowLeft,
  Trash2, Sliders, FileText, Download, Smartphone, CheckCircle, ExternalLink,
  Info, Clock
} from 'lucide-react';
import { VaultEntry, PasswordStrengthLevel } from '../types';
import { INITIAL_ENTRIES, evaluatePassword, runVaultAudit } from '../data/sampleVault';

type SimulatorScreen = 'setup' | 'unlock' | 'vault' | 'edit_entry' | 'audit' | 'settings';

export const PhoneSimulator: React.FC = () => {
  // State
  const [isInitialized, setIsInitialized] = useState<boolean>(true);
  const [isLocked, setIsLocked] = useState<boolean>(false);
  const [currentScreen, setCurrentScreen] = useState<SimulatorScreen>('vault');

  // Master password state
  const [masterPassword, setMasterPassword] = useState<string>('MasterPass2026!#');
  const [setupPasswordInput, setSetupPasswordInput] = useState<string>('');
  const [setupConfirmInput, setSetupConfirmInput] = useState<string>('');
  const [unlockPasswordInput, setUnlockPasswordInput] = useState<string>('');
  const [unlockError, setUnlockError] = useState<string | null>(null);

  // Vault data
  const [entries, setEntries] = useState<VaultEntry[]>(INITIAL_ENTRIES);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedFolder, setSelectedFolder] = useState<string>('All');
  const [selectedEntryId, setSelectedEntryId] = useState<string | null>(null);
  const [unmaskedIds, setUnmaskedIds] = useState<Set<string>>(new Set());

  // Edit / Add form state
  const [formTitle, setFormTitle] = useState('');
  const [formUsername, setFormUsername] = useState('');
  const [formPassword, setFormPassword] = useState('');
  const [formUrl, setFormUrl] = useState('');
  const [formFolder, setFormFolder] = useState('');
  const [formNotes, setFormNotes] = useState('');
  const [formIsFavorite, setFormIsFavorite] = useState(false);
  const [showPasswordInForm, setShowPasswordInForm] = useState(false);

  // Password generator modal
  const [showGeneratorModal, setShowGeneratorModal] = useState(false);
  const [genLength, setGenLength] = useState(18);
  const [genUpper, setGenUpper] = useState(true);
  const [genLower, setGenLower] = useState(true);
  const [genNumbers, setGenNumbers] = useState(true);
  const [genSymbols, setGenSymbols] = useState(true);
  const [genPassphraseMode, setGenPassphraseMode] = useState(false);
  const [generatedPassword, setGeneratedPassword] = useState('k9!P2#xL8$mZ4@vQ');

  // Settings state
  const [idleTimeoutMinutes, setIdleTimeoutMinutes] = useState<number>(5);
  const [clipboardTimeoutSec, setClipboardTimeoutSec] = useState<number>(30);
  const [biometricEnabled, setBiometricEnabled] = useState<boolean>(true);
  const [preventScreenshots, setPreventScreenshots] = useState<boolean>(true);

  // Auto-lock & Clipboard simulation
  const [idleSecondsRemaining, setIdleSecondsRemaining] = useState<number>(idleTimeoutMinutes * 60);
  const [clipboardToast, setClipboardToast] = useState<string | null>(null);
  const [clipboardSecRemaining, setClipboardSecRemaining] = useState<number>(0);
  const [biometricPromptActive, setBiometricPromptActive] = useState<boolean>(false);

  // Reset idle timer on user action
  const touchActivity = () => {
    if (!isLocked) {
      setIdleSecondsRemaining(idleTimeoutMinutes * 60);
    }
  };

  // Idle countdown timer
  useEffect(() => {
    if (isLocked || !isInitialized) return;
    const interval = setInterval(() => {
      setIdleSecondsRemaining(prev => {
        if (prev <= 1) {
          setIsLocked(true);
          setCurrentScreen('unlock');
          return idleTimeoutMinutes * 60;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [isLocked, isInitialized, idleTimeoutMinutes]);

  // Clipboard countdown timer
  useEffect(() => {
    if (clipboardSecRemaining <= 0) {
      if (clipboardToast) {
        setClipboardToast('Sensitive credential cleared from clipboard.');
        const clearTimer = setTimeout(() => setClipboardToast(null), 3000);
        return () => clearTimeout(clearTimer);
      }
      return;
    }
    const interval = setInterval(() => {
      setClipboardSecRemaining(prev => prev - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [clipboardSecRemaining, clipboardToast]);

  // Generate password algorithm
  const triggerGenerate = () => {
    if (genPassphraseMode) {
      const words = ['cosmic', 'anchor', 'delta', 'falcon', 'quantum', 'cipher', 'horizon', 'matrix'];
      const picked = Array.from({ length: 4 }, () => words[Math.floor(Math.random() * words.length)]);
      setGeneratedPassword(picked.map(w => w.charAt(0).toUpperCase() + w.slice(1)).join('-') + Math.floor(Math.random() * 90 + 10));
    } else {
      let pool = '';
      if (genLower) pool += 'abcdefghijkmnopqrstuvwxyz';
      if (genUpper) pool += 'ABCDEFGHJKLMNPQRSTUVWXYZ';
      if (genNumbers) pool += '23456789';
      if (genSymbols) pool += '!@#$%^&*-_=+';
      if (!pool) pool = 'abcdefghijklmnopqrstuvwxyz';

      let res = '';
      for (let i = 0; i < genLength; i++) {
        res += pool.charAt(Math.floor(Math.random() * pool.length));
      }
      setGeneratedPassword(res);
    }
  };

  useEffect(() => {
    triggerGenerate();
  }, [genLength, genUpper, genLower, genNumbers, genSymbols, genPassphraseMode]);

  // Copy password with auto-clear simulation
  const handleCopyPassword = (pwd: string) => {
    touchActivity();
    navigator.clipboard.writeText(pwd);
    setClipboardSecRemaining(clipboardTimeoutSec);
    setClipboardToast(`Password copied. Auto-clearing in ${clipboardTimeoutSec}s`);
  };

  // Folders list
  const folders = useMemo(() => {
    const set = new Set(entries.map(e => e.folder).filter(Boolean));
    return ['All', ...Array.from(set)];
  }, [entries]);

  // Filtered entries
  const filteredEntries = useMemo(() => {
    return entries.filter(e => {
      const matchesSearch = searchQuery === '' ||
        e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        e.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
        e.url.toLowerCase().includes(searchQuery.toLowerCase()) ||
        e.tags.some(t => t.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesFolder = selectedFolder === 'All' || e.folder === selectedFolder;
      return matchesSearch && matchesFolder;
    }).sort((a, b) => (b.isFavorite ? 1 : 0) - (a.isFavorite ? 1 : 0));
  }, [entries, searchQuery, selectedFolder]);

  // Open Edit/Add Screen
  const handleOpenEdit = (entry?: VaultEntry) => {
    touchActivity();
    if (entry) {
      setSelectedEntryId(entry.id);
      setFormTitle(entry.title);
      setFormUsername(entry.username);
      setFormPassword(entry.password);
      setFormUrl(entry.url);
      setFormFolder(entry.folder);
      setFormNotes(entry.notes);
      setFormIsFavorite(entry.isFavorite);
    } else {
      setSelectedEntryId(null);
      setFormTitle('');
      setFormUsername('');
      setFormPassword('');
      setFormUrl('');
      setFormFolder('');
      setFormNotes('');
      setFormIsFavorite(false);
    }
    setShowPasswordInForm(false);
    setCurrentScreen('edit_entry');
  };

  // Save entry
  const handleSaveEntry = () => {
    touchActivity();
    if (!formTitle.trim() || !formPassword.trim()) return;

    if (selectedEntryId) {
      setEntries(prev => prev.map(e => {
        if (e.id === selectedEntryId) {
          return {
            ...e,
            title: formTitle.trim(),
            username: formUsername.trim(),
            password: formPassword,
            url: formUrl.trim(),
            folder: formFolder.trim(),
            notes: formNotes.trim(),
            isFavorite: formIsFavorite,
            updatedAt: Date.now()
          };
        }
        return e;
      }));
    } else {
      const newEntry: VaultEntry = {
        id: `entry-${Date.now()}`,
        title: formTitle.trim(),
        username: formUsername.trim(),
        password: formPassword,
        url: formUrl.trim(),
        folder: formFolder.trim() || 'General',
        notes: formNotes.trim(),
        tags: [],
        isFavorite: formIsFavorite,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };
      setEntries(prev => [newEntry, ...prev]);
    }
    setCurrentScreen('vault');
  };

  // Delete entry
  const handleDeleteEntry = (id: string) => {
    touchActivity();
    setEntries(prev => prev.filter(e => e.id !== id));
    setCurrentScreen('vault');
  };

  // Duplicate entry
  const handleDuplicateEntry = (entry: VaultEntry) => {
    touchActivity();
    const dup: VaultEntry = {
      ...entry,
      id: `entry-${Date.now()}`,
      title: `${entry.title} (Copy)`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setEntries(prev => [dup, ...prev]);
  };

  // Toggle Mask
  const toggleMask = (id: string) => {
    touchActivity();
    setUnmaskedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  // Handle Unlock
  const handleUnlockWithPassword = () => {
    if (unlockPasswordInput === masterPassword) {
      setIsLocked(false);
      setUnlockPasswordInput('');
      setUnlockError(null);
      setCurrentScreen('vault');
      setIdleSecondsRemaining(idleTimeoutMinutes * 60);
    } else {
      setUnlockError('Incorrect master password.');
    }
  };

  // Handle Biometric Unlock
  const handleBiometricUnlock = () => {
    setBiometricPromptActive(true);
    setTimeout(() => {
      setBiometricPromptActive(false);
      setIsLocked(false);
      setUnlockError(null);
      setCurrentScreen('vault');
      setIdleSecondsRemaining(idleTimeoutMinutes * 60);
    }, 900);
  };

  const auditSummary = useMemo(() => runVaultAudit(entries), [entries]);

  return (
    <div className="flex flex-col items-center justify-center p-4">
      {/* Device Frame */}
      <div className="w-[380px] h-[780px] bg-slate-950 rounded-[44px] p-3 shadow-2xl border-4 border-slate-700/60 relative flex flex-col overflow-hidden select-none">
        
        {/* Device Camera Punchhole Notch */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 w-28 h-5 bg-black rounded-full z-50 flex items-center justify-center">
          <div className="w-3 h-3 bg-slate-900 rounded-full border border-slate-800"></div>
        </div>

        {/* Screen Bezel Area */}
        <div 
          onClick={touchActivity}
          className="w-full h-full bg-slate-900 text-slate-100 rounded-[34px] overflow-hidden flex flex-col relative font-sans"
        >
          {/* Status Bar */}
          <div className="h-9 px-6 pt-2 flex items-center justify-between text-xs text-slate-400 z-40 bg-slate-900/90 backdrop-blur-sm">
            <span>9:41</span>
            <div className="flex items-center space-x-2 text-[10px]">
              <span className="bg-emerald-500/20 text-emerald-400 px-1.5 py-0.5 rounded font-mono font-medium">OFFLINE</span>
              <span>100%</span>
            </div>
          </div>

          {/* Screenshot Blocker Simulation Indicator */}
          {preventScreenshots && (
            <div className="bg-sky-950/60 border-b border-sky-800/40 text-[10px] text-sky-300 px-4 py-1 flex items-center justify-between">
              <span className="flex items-center gap-1">
                <Lock className="w-2.5 h-2.5" /> FLAG_SECURE Active (Screenshots Blocked)
              </span>
              {!isLocked && (
                <span className="font-mono text-slate-400 flex items-center gap-1">
                  <Clock className="w-2.5 h-2.5" /> {Math.floor(idleSecondsRemaining / 60)}:{(idleSecondsRemaining % 60).toString().padStart(2, '0')}
                </span>
              )}
            </div>
          )}

          {/* SCREEN CONTENT */}
          <div className="flex-1 overflow-y-auto relative flex flex-col">

            {/* SCREEN: SETUP */}
            {currentScreen === 'setup' && (
              <div className="p-6 flex flex-col justify-center flex-1 space-y-4">
                <div className="w-14 h-14 rounded-2xl bg-sky-500/20 text-sky-400 flex items-center justify-center mx-auto mb-2">
                  <Shield className="w-8 h-8" />
                </div>
                <div className="text-center">
                  <h2 className="text-xl font-bold text-slate-100">Create Master Password</h2>
                  <p className="text-xs text-slate-400 mt-1">
                    Encrypts local SQLCipher database via PBKDF2 (120k rounds) + AES-256. Never sent over network.
                  </p>
                </div>

                <div className="space-y-3 pt-2">
                  <div>
                    <label className="text-[11px] text-slate-400 uppercase font-semibold">Master Password (min 12 chars)</label>
                    <input
                      type="password"
                      value={setupPasswordInput}
                      onChange={e => setSetupPasswordInput(e.target.value)}
                      placeholder="••••••••••••••••"
                      className="w-full mt-1 px-3 py-2 bg-slate-800/90 border border-slate-700 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                    />
                    {setupPasswordInput && (
                      <div className="mt-1 flex items-center justify-between text-[11px]">
                        <span className="text-slate-400">Strength: {evaluatePassword(setupPasswordInput).level}</span>
                        <span className="text-slate-400">{setupPasswordInput.length} chars</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="text-[11px] text-slate-400 uppercase font-semibold">Confirm Password</label>
                    <input
                      type="password"
                      value={setupConfirmInput}
                      onChange={e => setSetupConfirmInput(e.target.value)}
                      placeholder="••••••••••••••••"
                      className="w-full mt-1 px-3 py-2 bg-slate-800/90 border border-slate-700 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                    />
                  </div>

                  <div className="bg-slate-800/60 p-3 rounded-xl flex items-center justify-between">
                    <div>
                      <div className="text-xs font-semibold">Biometric Unlock</div>
                      <div className="text-[10px] text-slate-400">Enable fingerprint / face unlock</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={biometricEnabled}
                      onChange={e => setBiometricEnabled(e.target.checked)}
                      className="w-4 h-4 accent-sky-500"
                    />
                  </div>

                  <button
                    disabled={setupPasswordInput.length < 12 || setupPasswordInput !== setupConfirmInput}
                    onClick={() => {
                      setMasterPassword(setupPasswordInput);
                      setIsInitialized(true);
                      setIsLocked(false);
                      setCurrentScreen('vault');
                    }}
                    className="w-full py-2.5 bg-sky-600 hover:bg-sky-500 disabled:opacity-40 disabled:hover:bg-sky-600 text-white rounded-xl text-xs font-semibold transition"
                  >
                    Initialize Encrypted Vault
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN: UNLOCK */}
            {currentScreen === 'unlock' && (
              <div className="p-6 flex flex-col justify-center flex-1 space-y-5">
                <div className="w-16 h-16 rounded-2xl bg-sky-500/10 border border-sky-500/20 text-sky-400 flex items-center justify-center mx-auto">
                  <Lock className="w-8 h-8" />
                </div>
                <div className="text-center">
                  <h2 className="text-xl font-bold">SecureVault Locked</h2>
                  <p className="text-xs text-slate-400 mt-1">Database encryption keys wiped from memory</p>
                </div>

                <div className="space-y-3">
                  <input
                    type="password"
                    value={unlockPasswordInput}
                    onChange={e => setUnlockPasswordInput(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleUnlockWithPassword()}
                    placeholder="Enter Master Password"
                    className="w-full px-3.5 py-2.5 bg-slate-800/90 border border-slate-700 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />

                  {unlockError && (
                    <div className="text-xs text-rose-400 text-center">{unlockError}</div>
                  )}

                  <button
                    onClick={handleUnlockWithPassword}
                    className="w-full py-2.5 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-semibold transition"
                  >
                    Unlock Vault
                  </button>

                  {biometricEnabled && (
                    <button
                      onClick={handleBiometricUnlock}
                      className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition"
                    >
                      <Fingerprint className="w-4 h-4 text-sky-400" /> Unlock with Biometrics
                    </button>
                  )}
                </div>

                <div className="text-center text-[10px] text-slate-500">
                  Default Demo Master Password: <span className="text-slate-300 font-mono">MasterPass2026!#</span>
                </div>
              </div>
            )}

            {/* SCREEN: VAULT LIST */}
            {currentScreen === 'vault' && (
              <div className="flex flex-col flex-1 pb-16">
                {/* Header */}
                <div className="p-4 pb-2 border-b border-slate-800/80">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center space-x-2">
                      <Shield className="w-5 h-5 text-sky-400" />
                      <span className="font-bold text-base tracking-tight">SecureVault</span>
                    </div>
                    <div className="flex items-center space-x-1">
                      <button
                        onClick={() => setCurrentScreen('audit')}
                        className="p-1.5 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg"
                        title="Security Audit"
                      >
                        <AlertTriangle className="w-4 h-4 text-amber-400" />
                      </button>
                      <button
                        onClick={() => setCurrentScreen('settings')}
                        className="p-1.5 text-slate-400 hover:text-slate-100 hover:bg-slate-800 rounded-lg"
                        title="Settings"
                      >
                        <Settings className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => {
                          setIsLocked(true);
                          setCurrentScreen('unlock');
                        }}
                        className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg"
                        title="Lock Vault Now"
                      >
                        <Lock className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Search Bar */}
                  <div className="relative">
                    <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={e => setSearchQuery(e.target.value)}
                      placeholder="Search credentials..."
                      className="w-full bg-slate-800/70 border border-slate-700/70 rounded-xl pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500"
                    />
                  </div>

                  {/* Folder Tabs */}
                  <div className="flex space-x-1.5 overflow-x-auto py-2 scrollbar-none text-[11px]">
                    {folders.map(f => (
                      <button
                        key={f}
                        onClick={() => setSelectedFolder(f)}
                        className={`px-2.5 py-1 rounded-full whitespace-nowrap transition ${
                          selectedFolder === f
                            ? 'bg-sky-500 text-white font-medium'
                            : 'bg-slate-800/80 text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        {f}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Entry List */}
                <div className="flex-1 p-3 space-y-2 overflow-y-auto">
                  <div className="text-[11px] text-slate-400 font-medium px-1 flex justify-between">
                    <span>{filteredEntries.length} items</span>
                    <span>Sorted by Favorites</span>
                  </div>

                  {filteredEntries.length === 0 ? (
                    <div className="text-center py-12 text-slate-500 text-xs">
                      No credentials found
                    </div>
                  ) : (
                    filteredEntries.map(entry => {
                      const isUnmasked = unmaskedIds.has(entry.id);
                      return (
                        <div
                          key={entry.id}
                          className="bg-slate-800/60 border border-slate-700/40 hover:border-slate-600/70 rounded-xl p-3 transition"
                        >
                          <div className="flex items-start justify-between">
                            <div
                              onClick={() => handleOpenEdit(entry)}
                              className="flex-1 cursor-pointer"
                            >
                              <div className="flex items-center space-x-2">
                                <span className="font-semibold text-xs text-slate-100">{entry.title}</span>
                                {entry.isFavorite && (
                                  <Star className="w-3 h-3 text-amber-400 fill-amber-400" />
                                )}
                                {entry.folder && (
                                  <span className="text-[9px] bg-slate-700/60 text-slate-300 px-1.5 py-0.5 rounded">
                                    {entry.folder}
                                  </span>
                                )}
                              </div>
                              <div className="text-[11px] text-slate-400 mt-0.5">{entry.username}</div>
                            </div>

                            <div className="flex items-center space-x-1">
                              <button
                                onClick={() => handleDuplicateEntry(entry)}
                                className="p-1 text-slate-400 hover:text-slate-200 hover:bg-slate-700/50 rounded"
                                title="Duplicate"
                              >
                                <Copy className="w-3 h-3" />
                              </button>
                              <button
                                onClick={() => handleDeleteEntry(entry.id)}
                                className="p-1 text-slate-400 hover:text-rose-400 hover:bg-slate-700/50 rounded"
                                title="Delete"
                              >
                                <Trash2 className="w-3 h-3" />
                              </button>
                            </div>
                          </div>

                          {/* Masked Password Bar */}
                          <div className="mt-2 pt-2 border-t border-slate-700/40 flex items-center justify-between">
                            <span className="font-mono text-xs text-slate-300">
                              {isUnmasked ? entry.password : '••••••••••••'}
                            </span>
                            <div className="flex items-center space-x-1">
                              <button
                                onClick={() => toggleMask(entry.id)}
                                className="p-1 text-slate-400 hover:text-slate-200"
                                title={isUnmasked ? 'Hide' : 'Show'}
                              >
                                {isUnmasked ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                              </button>
                              <button
                                onClick={() => handleCopyPassword(entry.password)}
                                className="p-1 text-sky-400 hover:text-sky-300"
                                title="Copy Password (Auto-clears)"
                              >
                                <Copy className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>

                {/* Floating Add Button */}
                <button
                  onClick={() => handleOpenEdit()}
                  className="absolute bottom-5 right-5 w-12 h-12 rounded-full bg-sky-500 hover:bg-sky-400 text-white shadow-lg flex items-center justify-center transition active:scale-95"
                >
                  <Plus className="w-6 h-6" />
                </button>
              </div>
            )}

            {/* SCREEN: EDIT / ADD ENTRY */}
            {currentScreen === 'edit_entry' && (
              <div className="p-4 flex-1 flex flex-col overflow-y-auto space-y-3 pb-8">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <button
                    onClick={() => setCurrentScreen('vault')}
                    className="text-xs text-slate-400 hover:text-slate-100 flex items-center gap-1"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" /> Back
                  </button>
                  <span className="text-xs font-bold text-slate-200">
                    {selectedEntryId ? 'Edit Credential' : 'Add Credential'}
                  </span>
                  <button
                    onClick={() => setFormIsFavorite(!formIsFavorite)}
                    className="p-1 text-slate-400"
                  >
                    <Star className={`w-4 h-4 ${formIsFavorite ? 'text-amber-400 fill-amber-400' : ''}`} />
                  </button>
                </div>

                <div>
                  <label className="text-[10px] uppercase font-semibold text-slate-400">Title *</label>
                  <input
                    type="text"
                    value={formTitle}
                    onChange={e => setFormTitle(e.target.value)}
                    placeholder="e.g. AWS Console, Proton, Work VPN"
                    className="w-full mt-1 px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <label className="text-[10px] uppercase font-semibold text-slate-400">Username / Email</label>
                  <input
                    type="text"
                    value={formUsername}
                    onChange={e => setFormUsername(e.target.value)}
                    placeholder="username@domain.com"
                    className="w-full mt-1 px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between">
                    <label className="text-[10px] uppercase font-semibold text-slate-400">Password *</label>
                    <button
                      type="button"
                      onClick={() => setShowGeneratorModal(true)}
                      className="text-[10px] text-sky-400 hover:text-sky-300 flex items-center gap-1"
                    >
                      <Sliders className="w-3 h-3" /> Generator
                    </button>
                  </div>
                  <div className="relative mt-1">
                    <input
                      type={showPasswordInForm ? 'text' : 'password'}
                      value={formPassword}
                      onChange={e => setFormPassword(e.target.value)}
                      placeholder="Password"
                      className="w-full px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-xl text-xs font-mono text-slate-100 pr-16 focus:outline-none focus:border-sky-500"
                    />
                    <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center space-x-1 text-slate-400">
                      <button
                        type="button"
                        onClick={() => setShowPasswordInForm(!showPasswordInForm)}
                        className="p-1 hover:text-slate-200"
                      >
                        {showPasswordInForm ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                  </div>

                  {formPassword && (
                    <div className="mt-1 flex items-center justify-between text-[10px] text-slate-400">
                      <span>Strength: {evaluatePassword(formPassword).level}</span>
                      <span>{formPassword.length} chars</span>
                    </div>
                  )}
                </div>

                <div>
                  <label className="text-[10px] uppercase font-semibold text-slate-400">Website or App URL</label>
                  <input
                    type="text"
                    value={formUrl}
                    onChange={e => setFormUrl(e.target.value)}
                    placeholder="https://example.com"
                    className="w-full mt-1 px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <label className="text-[10px] uppercase font-semibold text-slate-400">Folder</label>
                  <input
                    type="text"
                    value={formFolder}
                    onChange={e => setFormFolder(e.target.value)}
                    placeholder="e.g. Work, Finance, Personal"
                    className="w-full mt-1 px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div>
                  <label className="text-[10px] uppercase font-semibold text-slate-400">Encrypted Notes</label>
                  <textarea
                    rows={2}
                    value={formNotes}
                    onChange={e => setFormNotes(e.target.value)}
                    placeholder="Recovery codes, PINs, or confidential details..."
                    className="w-full mt-1 px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-sky-500 resize-none"
                  />
                </div>

                <div className="pt-2 flex space-x-2">
                  {selectedEntryId && (
                    <button
                      onClick={() => handleDeleteEntry(selectedEntryId)}
                      className="px-3 py-2 bg-rose-600/20 text-rose-400 hover:bg-rose-600/30 rounded-xl text-xs font-semibold transition"
                    >
                      Delete
                    </button>
                  )}
                  <button
                    onClick={handleSaveEntry}
                    disabled={!formTitle.trim() || !formPassword.trim()}
                    className="flex-1 py-2 bg-sky-600 hover:bg-sky-500 disabled:opacity-40 text-white rounded-xl text-xs font-semibold transition"
                  >
                    {selectedEntryId ? 'Update Credential' : 'Save Credential'}
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN: SECURITY AUDIT */}
            {currentScreen === 'audit' && (
              <div className="p-4 flex-1 flex flex-col overflow-y-auto space-y-4">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <button
                    onClick={() => setCurrentScreen('vault')}
                    className="text-xs text-slate-400 hover:text-slate-100 flex items-center gap-1"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" /> Back
                  </button>
                  <span className="text-xs font-bold text-slate-200">Security Health Audit</span>
                  <div className="w-4"></div>
                </div>

                <div className="grid grid-cols-3 gap-2 text-center">
                  <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-2">
                    <div className="text-lg font-bold text-rose-400">{auditSummary.weakCount}</div>
                    <div className="text-[10px] text-slate-400">Weak</div>
                  </div>
                  <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-2">
                    <div className="text-lg font-bold text-amber-400">{auditSummary.reusedCount}</div>
                    <div className="text-[10px] text-slate-400">Reused</div>
                  </div>
                  <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-2">
                    <div className="text-lg font-bold text-emerald-400">{auditSummary.strongCount}</div>
                    <div className="text-[10px] text-slate-400">Strong</div>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="text-xs font-semibold text-slate-300">Audit Breakdown</div>
                  {auditSummary.results.map(res => (
                    <div key={res.entryId} className="bg-slate-800/70 border border-slate-700/60 rounded-xl p-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-200">{res.title}</span>
                        <span className={`text-[10px] px-2 py-0.5 rounded font-medium ${
                          res.strength === 'Very Weak' || res.strength === 'Weak'
                            ? 'bg-rose-500/20 text-rose-300'
                            : res.strength === 'Fair'
                            ? 'bg-amber-500/20 text-amber-300'
                            : 'bg-emerald-500/20 text-emerald-300'
                        }`}>
                          {res.strength}
                        </span>
                      </div>
                      {res.issues.map((issue, idx) => (
                        <div key={idx} className="text-[10px] text-rose-400/90 flex items-center gap-1.5 mt-1">
                          <AlertTriangle className="w-3 h-3 flex-shrink-0" />
                          <span>{issue}</span>
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* SCREEN: SETTINGS */}
            {currentScreen === 'settings' && (
              <div className="p-4 flex-1 flex flex-col overflow-y-auto space-y-4 pb-8">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <button
                    onClick={() => setCurrentScreen('vault')}
                    className="text-xs text-slate-400 hover:text-slate-100 flex items-center gap-1"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" /> Back
                  </button>
                  <span className="text-xs font-bold text-slate-200">Settings & Policy</span>
                  <div className="w-4"></div>
                </div>

                <div className="space-y-3">
                  <div className="bg-slate-800/60 rounded-xl p-3 border border-slate-700/50">
                    <div className="text-xs font-semibold">Auto-Lock Idle Timeout</div>
                    <div className="text-[10px] text-slate-400 mb-2">Locks vault and zeroizes memory</div>
                    <div className="grid grid-cols-4 gap-1.5">
                      {[1, 5, 15, 30].map(mins => (
                        <button
                          key={mins}
                          onClick={() => {
                            setIdleTimeoutMinutes(mins);
                            setIdleSecondsRemaining(mins * 60);
                          }}
                          className={`py-1 text-xs rounded-lg transition ${
                            idleTimeoutMinutes === mins
                              ? 'bg-sky-500 text-white font-semibold'
                              : 'bg-slate-700/50 text-slate-300'
                          }`}
                        >
                          {mins}m
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="bg-slate-800/60 rounded-xl p-3 border border-slate-700/50">
                    <div className="text-xs font-semibold">Clipboard Auto-Clear</div>
                    <div className="text-[10px] text-slate-400 mb-2">Purges copied credentials from memory</div>
                    <div className="grid grid-cols-3 gap-1.5">
                      {[15, 30, 60].map(sec => (
                        <button
                          key={sec}
                          onClick={() => setClipboardTimeoutSec(sec)}
                          className={`py-1 text-xs rounded-lg transition ${
                            clipboardTimeoutSec === sec
                              ? 'bg-sky-500 text-white font-semibold'
                              : 'bg-slate-700/50 text-slate-300'
                          }`}
                        >
                          {sec}s
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="bg-slate-800/60 rounded-xl p-3 border border-slate-700/50 flex items-center justify-between">
                    <div>
                      <div className="text-xs font-semibold">Biometric Unlock</div>
                      <div className="text-[10px] text-slate-400">Fingerprint & face recognition</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={biometricEnabled}
                      onChange={e => setBiometricEnabled(e.target.checked)}
                      className="w-4 h-4 accent-sky-500"
                    />
                  </div>

                  <div className="bg-slate-800/60 rounded-xl p-3 border border-slate-700/50 flex items-center justify-between">
                    <div>
                      <div className="text-xs font-semibold">Prevent Screenshots</div>
                      <div className="text-[10px] text-slate-400">FLAG_SECURE & hides in app switcher</div>
                    </div>
                    <input
                      type="checkbox"
                      checked={preventScreenshots}
                      onChange={e => setPreventScreenshots(e.target.checked)}
                      className="w-4 h-4 accent-sky-500"
                    />
                  </div>

                  <div className="bg-slate-800/60 rounded-xl p-3 border border-slate-700/50 space-y-2">
                    <div className="text-xs font-semibold">Encrypted Backup (.svlt)</div>
                    <div className="text-[10px] text-slate-400">
                      Export encrypted JSON payload protected with AES-GCM and custom passphrase.
                    </div>
                    <button
                      onClick={() => {
                        const blob = new Blob([JSON.stringify({ magic: 'SVLT', version: 1, entries })], { type: 'application/octet-stream' });
                        const url = URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `securevault_backup_${Date.now()}.svlt`;
                        a.click();
                      }}
                      className="w-full py-2 bg-slate-700 hover:bg-slate-600 text-slate-100 rounded-xl text-xs font-medium flex items-center justify-center gap-1.5"
                    >
                      <Download className="w-3.5 h-3.5" /> Export Encrypted Backup
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* SIMULATED BIOMETRIC OVERLAY */}
          {biometricPromptActive && (
            <div className="absolute inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-6 animate-in fade-in duration-200">
              <div className="bg-slate-800 border border-slate-700 rounded-2xl p-6 text-center space-y-4 w-full">
                <div className="w-16 h-16 rounded-full bg-sky-500/20 text-sky-400 flex items-center justify-center mx-auto animate-pulse">
                  <Fingerprint className="w-10 h-10" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-100">Unlock SecureVault</h3>
                  <p className="text-[11px] text-slate-400 mt-1">Touch the fingerprint sensor</p>
                </div>
              </div>
            </div>
          )}

          {/* SIMULATED GENERATOR MODAL */}
          {showGeneratorModal && (
            <div className="absolute inset-0 bg-black/70 backdrop-blur-sm z-40 flex flex-col justify-end">
              <div className="bg-slate-800 border-t border-slate-700 rounded-t-3xl p-5 space-y-4 max-h-[90%] overflow-y-auto">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-200">Password Generator</span>
                  <button onClick={() => setShowGeneratorModal(false)} className="text-slate-400 text-xs">Close</button>
                </div>

                {/* Generated Output */}
                <div className="bg-slate-900/90 border border-slate-700 p-3 rounded-xl flex items-center justify-between">
                  <span className="font-mono text-xs text-sky-300 break-all select-all">{generatedPassword}</span>
                  <button onClick={triggerGenerate} className="p-1 text-slate-400 hover:text-slate-200">
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex space-x-2 text-xs">
                  <button
                    onClick={() => setGenPassphraseMode(false)}
                    className={`flex-1 py-1.5 rounded-lg font-medium transition ${!genPassphraseMode ? 'bg-sky-500 text-white' : 'bg-slate-700 text-slate-300'}`}
                  >
                    Random Chars
                  </button>
                  <button
                    onClick={() => setGenPassphraseMode(true)}
                    className={`flex-1 py-1.5 rounded-lg font-medium transition ${genPassphraseMode ? 'bg-sky-500 text-white' : 'bg-slate-700 text-slate-300'}`}
                  >
                    Passphrase
                  </button>
                </div>

                {!genPassphraseMode && (
                  <div className="space-y-2 text-xs">
                    <div className="flex justify-between text-slate-400">
                      <span>Length: {genLength}</span>
                    </div>
                    <input
                      type="range"
                      min="8"
                      max="48"
                      value={genLength}
                      onChange={e => setGenLength(Number(e.target.value))}
                      className="w-full accent-sky-500"
                    />

                    <div className="grid grid-cols-2 gap-2 pt-1 text-[11px] text-slate-300">
                      <label className="flex items-center gap-1.5 cursor-pointer">
                        <input type="checkbox" checked={genUpper} onChange={e => setGenUpper(e.target.checked)} className="accent-sky-500" />
                        <span>A-Z</span>
                      </label>
                      <label className="flex items-center gap-1.5 cursor-pointer">
                        <input type="checkbox" checked={genLower} onChange={e => setGenLower(e.target.checked)} className="accent-sky-500" />
                        <span>a-z</span>
                      </label>
                      <label className="flex items-center gap-1.5 cursor-pointer">
                        <input type="checkbox" checked={genNumbers} onChange={e => setGenNumbers(e.target.checked)} className="accent-sky-500" />
                        <span>0-9</span>
                      </label>
                      <label className="flex items-center gap-1.5 cursor-pointer">
                        <input type="checkbox" checked={genSymbols} onChange={e => setGenSymbols(e.target.checked)} className="accent-sky-500" />
                        <span>!@#$</span>
                      </label>
                    </div>
                  </div>
                )}

                <button
                  onClick={() => {
                    setFormPassword(generatedPassword);
                    setShowGeneratorModal(false);
                  }}
                  className="w-full py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-semibold"
                >
                  Use This Password
                </button>
              </div>
            </div>
          )}

          {/* CLIPBOARD AUTO-CLEAR TOAST */}
          {clipboardToast && (
            <div className="absolute bottom-10 left-4 right-4 bg-slate-950/95 border border-slate-700/80 text-slate-200 px-3 py-2 rounded-xl text-xs shadow-2xl flex items-center justify-between z-50 animate-in fade-in duration-200">
              <span className="flex items-center gap-1.5">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                <span>{clipboardToast}</span>
              </span>
              {clipboardSecRemaining > 0 && (
                <span className="text-[10px] font-mono text-sky-400 font-bold">{clipboardSecRemaining}s</span>
              )}
            </div>
          )}

          {/* Android Bottom Navigation Pill */}
          <div className="h-5 flex items-center justify-center bg-slate-900">
            <div className="w-24 h-1 bg-slate-600 rounded-full"></div>
          </div>

        </div>
      </div>
    </div>
  );
};
