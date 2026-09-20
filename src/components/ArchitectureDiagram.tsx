import React from 'react';
import { Shield, Key, Lock, Database, Smartphone, AlertTriangle, CheckCircle, ArrowRight } from 'lucide-react';

export const ArchitectureDiagram: React.FC = () => {
  return (
    <div className="h-full overflow-y-auto space-y-6 p-2">
      {/* Cryptographic Key Pipeline */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 space-y-4">
        <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
          <Key className="w-4 h-4 text-sky-400" />
          Cryptographic Key Hierarchy & Memory Lifecycle
        </h3>
        
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
          <div className="bg-slate-950/60 border border-slate-800 p-3.5 rounded-xl space-y-2">
            <div className="flex items-center gap-1.5 text-sky-400 font-semibold">
              <span className="w-5 h-5 rounded-full bg-sky-500/20 flex items-center justify-center text-[10px]">1</span>
              Master Password
            </div>
            <p className="text-slate-400 text-[11px] leading-relaxed">
              Supplied by user as <code className="text-sky-300">CharArray</code>. Zeroized with nulls immediately following key derivation.
            </p>
          </div>

          <div className="bg-slate-950/60 border border-slate-800 p-3.5 rounded-xl space-y-2">
            <div className="flex items-center gap-1.5 text-sky-400 font-semibold">
              <span className="w-5 h-5 rounded-full bg-sky-500/20 flex items-center justify-center text-[10px]">2</span>
              PBKDF2-HMAC-SHA256
            </div>
            <p className="text-slate-400 text-[11px] leading-relaxed">
              120,000 iterations + 32-byte secure random salt derives a 256-bit AES master key in memory.
            </p>
          </div>

          <div className="bg-slate-950/60 border border-slate-800 p-3.5 rounded-xl space-y-2">
            <div className="flex items-center gap-1.5 text-sky-400 font-semibold">
              <span className="w-5 h-5 rounded-full bg-sky-500/20 flex items-center justify-center text-[10px]">3</span>
              Android Keystore
            </div>
            <p className="text-slate-400 text-[11px] leading-relaxed">
              Hardware-backed AES-256-GCM key wraps the database passphrase. Supported by TEE / StrongBox.
            </p>
          </div>

          <div className="bg-slate-950/60 border border-slate-800 p-3.5 rounded-xl space-y-2">
            <div className="flex items-center gap-1.5 text-emerald-400 font-semibold">
              <span className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center text-[10px]">4</span>
              SQLCipher Room DB
            </div>
            <p className="text-slate-400 text-[11px] leading-relaxed">
              Full database page encryption (AES-256-CBC + HMAC-SHA512). Decryption key held in transient memory only.
            </p>
          </div>
        </div>
      </div>

      {/* Threat Matrix */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 space-y-4">
        <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
          <Shield className="w-4 h-4 text-emerald-400" />
          Threat Mitigation Matrix
        </h3>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400">
                <th className="py-2.5 px-3 font-semibold">Threat Vector</th>
                <th className="py-2.5 px-3 font-semibold">Mitigation Strategy</th>
                <th className="py-2.5 px-3 font-semibold">Implementation Detail</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              <tr>
                <td className="py-2.5 px-3 font-medium text-slate-200">Device Theft (At-Rest)</td>
                <td className="py-2.5 px-3 text-slate-400">AES-256 SQLCipher Database Encryption</td>
                <td className="py-2.5 px-3 font-mono text-[11px] text-sky-400">SupportFactory(key)</td>
              </tr>
              <tr>
                <td className="py-2.5 px-3 font-medium text-slate-200">Network Exfiltration</td>
                <td className="py-2.5 px-3 text-slate-400">Zero Network Permissions Declared</td>
                <td className="py-2.5 px-3 font-mono text-[11px] text-sky-400">No INTERNET permission</td>
              </tr>
              <tr>
                <td className="py-2.5 px-3 font-medium text-slate-200">App Switcher Snooping</td>
                <td className="py-2.5 px-3 text-slate-400">Prevents screenshots & obscures preview</td>
                <td className="py-2.5 px-3 font-mono text-[11px] text-sky-400">FLAG_SECURE</td>
              </tr>
              <tr>
                <td className="py-2.5 px-3 font-medium text-slate-200">Clipboard Snooping</td>
                <td className="py-2.5 px-3 text-slate-400">Auto-clears clipboard + hides preview</td>
                <td className="py-2.5 px-3 font-mono text-[11px] text-sky-400">EXTRA_IS_SENSITIVE</td>
              </tr>
              <tr>
                <td className="py-2.5 px-3 font-medium text-slate-200">Session Hijack (Unattended)</td>
                <td className="py-2.5 px-3 text-slate-400">Idle auto-lock & key zeroization</td>
                <td className="py-2.5 px-3 font-mono text-[11px] text-sky-400">IdleTimeoutTracker</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Security Disclaimer */}
      <div className="bg-amber-500/10 border border-amber-500/20 rounded-2xl p-4 flex items-start space-x-3 text-xs text-amber-200/90">
        <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-amber-300">Security Disclaimer: </span>
          SecureVault is an educational reference implementation. It assumes an unrooted, tamper-free Android device. For production environments handling high-value credentials, conduct an independent security audit of the target hardware and Keystore implementation.
        </div>
      </div>
    </div>
  );
};
