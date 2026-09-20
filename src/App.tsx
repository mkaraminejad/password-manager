import React, { useState } from 'react';
import { PhoneSimulator } from './components/PhoneSimulator';
import { CodeExplorer } from './components/CodeExplorer';
import { TestRunner } from './components/TestRunner';
import { ArchitectureDiagram } from './components/ArchitectureDiagram';
import { exportProjectZip } from './utils/exportProjectZip';
import {
  Shield, Smartphone, Code2, Terminal, Network, Download, CheckCircle2,
  Lock, Key, ExternalLink, RefreshCw
} from 'lucide-react';

type Tab = 'simulator' | 'code' | 'tests' | 'architecture';

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('simulator');
  const [isDownloading, setIsDownloading] = useState<boolean>(false);
  const [downloadSuccess, setDownloadSuccess] = useState<boolean>(false);

  const handleDownloadZip = async () => {
    try {
      setIsDownloading(true);
      await exportProjectZip();
      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 3000);
    } catch (err) {
      console.error('Download failed', err);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Header */}
      <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
              <Shield className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-base font-bold text-slate-100">SecureVault</h1>
                <span className="text-[10px] uppercase font-bold tracking-wider bg-sky-500/20 text-sky-400 border border-sky-500/30 px-2 py-0.5 rounded-full">
                  Android Native • Min SDK 26
                </span>
              </div>
              <p className="text-xs text-slate-400 hidden sm:block">
                Offline-First • SQLCipher AES-256 • Keystore Protected • Zero Internet Permission
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={handleDownloadZip}
              disabled={isDownloading}
              className="px-3.5 py-1.5 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white rounded-xl text-xs font-semibold flex items-center space-x-1.5 shadow-md shadow-sky-600/20 transition active:scale-95"
            >
              {downloadSuccess ? (
                <>
                  <CheckCircle2 className="w-4 h-4 text-emerald-300" />
                  <span>Downloaded!</span>
                </>
              ) : isDownloading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Packing ZIP...</span>
                </>
              ) : (
                <>
                  <Download className="w-4 h-4" />
                  <span className="hidden md:inline">Download Android Project (.zip)</span>
                  <span className="md:hidden">Get ZIP</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 flex space-x-1 overflow-x-auto text-xs font-medium border-t border-slate-800/60 pt-1">
          <button
            onClick={() => setActiveTab('simulator')}
            className={`flex items-center space-x-2 px-3.5 py-2.5 border-b-2 transition ${
              activeTab === 'simulator'
                ? 'border-sky-500 text-sky-400 font-semibold'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            <span>Interactive Simulator</span>
          </button>

          <button
            onClick={() => setActiveTab('code')}
            className={`flex items-center space-x-2 px-3.5 py-2.5 border-b-2 transition ${
              activeTab === 'code'
                ? 'border-sky-500 text-sky-400 font-semibold'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Code2 className="w-4 h-4" />
            <span>Android Kotlin Codebase</span>
          </button>

          <button
            onClick={() => setActiveTab('tests')}
            className={`flex items-center space-x-2 px-3.5 py-2.5 border-b-2 transition ${
              activeTab === 'tests'
                ? 'border-sky-500 text-sky-400 font-semibold'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Terminal className="w-4 h-4" />
            <span>Unit Tests Runner</span>
          </button>

          <button
            onClick={() => setActiveTab('architecture')}
            className={`flex items-center space-x-2 px-3.5 py-2.5 border-b-2 transition ${
              activeTab === 'architecture'
                ? 'border-sky-500 text-sky-400 font-semibold'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Network className="w-4 h-4" />
            <span>Threat Model & Architecture</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 flex flex-col overflow-hidden">
        {activeTab === 'simulator' && (
          <div className="flex flex-col lg:flex-row gap-6 items-center lg:items-start justify-center flex-1">
            {/* Phone Mockup */}
            <PhoneSimulator />

            {/* Side Explanations & Quick Feature Tour */}
            <div className="w-full lg:w-96 space-y-4 text-xs">
              <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 space-y-3">
                <div className="font-bold text-sm text-slate-100 flex items-center gap-2">
                  <Shield className="w-4 h-4 text-sky-400" />
                  Live Android Security Features
                </div>
                <ul className="space-y-2 text-slate-400">
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mt-1.5 flex-shrink-0"></span>
                    <span><strong className="text-slate-200">PBKDF2 Key Derivation:</strong> 120,000 rounds with 32-byte salt and in-memory CharArray zeroing.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mt-1.5 flex-shrink-0"></span>
                    <span><strong className="text-slate-200">SQLCipher Room DB:</strong> AES-256 encrypted local SQLite database with zero cloud sync.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mt-1.5 flex-shrink-0"></span>
                    <span><strong className="text-slate-200">FLAG_SECURE:</strong> Blocks screenshots and obscures credential previews in Android recent apps switcher.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mt-1.5 flex-shrink-0"></span>
                    <span><strong className="text-slate-200">Clipboard Auto-Clear:</strong> Background coroutine automatically purges copied passwords after timeout.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mt-1.5 flex-shrink-0"></span>
                    <span><strong className="text-slate-200">BiometricPrompt:</strong> Fingerprint & Face unlock with fallback to system credentials.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mt-1.5 flex-shrink-0"></span>
                    <span><strong className="text-slate-200">Inactivity Auto-Lock:</strong> Zeroizes transient memory keys upon backgrounding or idle timeout.</span>
                  </li>
                </ul>
              </div>

              <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 space-y-2">
                <div className="font-bold text-slate-100 flex items-center gap-2">
                  <Terminal className="w-3.5 h-3.5 text-emerald-400" />
                  Try in the Simulator
                </div>
                <div className="text-slate-400 space-y-1.5">
                  <p>1. Copy any password to see the real-time clipboard countdown toast.</p>
                  <p>2. Tap the <strong>Audit</strong> icon in the phone top-bar to see weak/reused password detection.</p>
                  <p>3. Tap <strong>+</strong> and open the <strong>Generator</strong> for passphrase and symbol modes.</p>
                  <p>4. Tap the <strong>Lock</strong> icon to simulate immediate database key zeroization.</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'code' && (
          <div className="flex-1 h-[750px]">
            <CodeExplorer />
          </div>
        )}

        {activeTab === 'tests' && (
          <div className="flex-1 h-[750px]">
            <TestRunner />
          </div>
        )}

        {activeTab === 'architecture' && (
          <div className="flex-1">
            <ArchitectureDiagram />
          </div>
        )}
      </main>
    </div>
  );
}
