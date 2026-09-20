import React, { useState } from 'react';
import { ANDROID_PROJECT_FILES } from '../data/projectFiles';
import { AndroidFileItem } from '../types';
import { Copy, Check, FileCode, Folder, Shield, Database, Layout, Terminal } from 'lucide-react';

export const CodeExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<AndroidFileItem>(ANDROID_PROJECT_FILES[4]); // CryptoManager.kt
  const [copied, setCopied] = useState<boolean>(false);
  const [categoryFilter, setCategoryFilter] = useState<string>('all');

  const filteredFiles = ANDROID_PROJECT_FILES.filter(f =>
    categoryFilter === 'all' ? true : f.category === categoryFilter
  );

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'core': return <Shield className="w-3.5 h-3.5 text-sky-400" />;
      case 'data': return <Database className="w-3.5 h-3.5 text-emerald-400" />;
      case 'ui': return <Layout className="w-3.5 h-3.5 text-violet-400" />;
      case 'test': return <Terminal className="w-3.5 h-3.5 text-amber-400" />;
      default: return <FileCode className="w-3.5 h-3.5 text-slate-400" />;
    }
  };

  return (
    <div className="flex flex-col lg:flex-row h-full rounded-2xl border border-slate-800 bg-slate-900/90 overflow-hidden">
      {/* File Tree Sidebar */}
      <div className="w-full lg:w-72 border-r border-slate-800 flex flex-col bg-slate-950/60">
        <div className="p-3 border-b border-slate-800">
          <div className="text-xs font-bold text-slate-200 uppercase tracking-wider mb-2">
            Android Source Tree
          </div>
          <div className="flex flex-wrap gap-1">
            {['all', 'core', 'data', 'ui', 'test', 'config'].map(cat => (
              <button
                key={cat}
                onClick={() => setCategoryFilter(cat)}
                className={`px-2 py-0.5 text-[10px] rounded-md uppercase font-semibold transition ${
                  categoryFilter === cat
                    ? 'bg-sky-500 text-white'
                    : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {filteredFiles.map(file => {
            const fileName = file.path.split('/').pop() || file.path;
            const isSelected = selectedFile.path === file.path;
            return (
              <button
                key={file.path}
                onClick={() => setSelectedFile(file)}
                className={`w-full text-left px-2.5 py-1.5 rounded-lg text-xs flex items-center space-x-2 transition ${
                  isSelected
                    ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`}
              >
                {getCategoryIcon(file.category)}
                <span className="truncate font-mono text-[11px]">{fileName}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Code Viewer */}
      <div className="flex-1 flex flex-col bg-slate-900 overflow-hidden">
        <div className="p-3 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
          <div>
            <div className="text-xs font-mono text-slate-200 font-semibold">{selectedFile.path}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">{selectedFile.description}</div>
          </div>
          <button
            onClick={handleCopy}
            className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs flex items-center space-x-1.5 transition"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>
        </div>

        <div className="flex-1 p-4 overflow-auto font-mono text-xs text-slate-300 leading-relaxed bg-slate-950/40 select-text">
          <pre>
            <code>{selectedFile.content}</code>
          </pre>
        </div>
      </div>
    </div>
  );
};
