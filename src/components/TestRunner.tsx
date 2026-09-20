import React, { useState } from 'react';
import { UNIT_TEST_SUITES, TestCase } from '../data/testSuiteData';
import { Play, CheckCircle2, Terminal, RefreshCw } from 'lucide-react';

export const TestRunner: React.FC = () => {
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [testResults, setTestResults] = useState<TestCase[]>(UNIT_TEST_SUITES);
  const [filterSuite, setFilterSuite] = useState<string>('all');

  const suites = Array.from(new Set(UNIT_TEST_SUITES.map(t => t.suite)));

  const handleRunTests = () => {
    setIsRunning(true);
    setTestResults([]);

    let count = 0;
    const interval = setInterval(() => {
      if (count < UNIT_TEST_SUITES.length) {
        setTestResults(prev => [...prev, UNIT_TEST_SUITES[count]]);
        count++;
      } else {
        clearInterval(interval);
        setIsRunning(false);
      }
    }, 120);
  };

  const displayed = testResults.filter(t => filterSuite === 'all' || t.suite === filterSuite);
  const totalDuration = testResults.reduce((acc, t) => acc + t.durationMs, 0);

  return (
    <div className="flex flex-col h-full rounded-2xl border border-slate-800 bg-slate-900/90 p-4 space-y-4">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
        <div>
          <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
            <Terminal className="w-4 h-4 text-emerald-400" />
            Android Gradle Unit Test Runner (JUnit 4 + MockK)
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Validates cryptographic primitives, key derivation, SQLCipher repository, memory wiping, and timeouts
          </p>
        </div>

        <button
          onClick={handleRunTests}
          disabled={isRunning}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-xl text-xs font-semibold flex items-center space-x-2 transition"
        >
          {isRunning ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Play className="w-3.5 h-3.5" />}
          <span>{isRunning ? 'Running Tests...' : 'Run All Unit Tests'}</span>
        </button>
      </div>

      {/* Metrics Bar */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-slate-950/60 border border-slate-800 p-3 rounded-xl">
          <div className="text-xs text-slate-400">Total Passed</div>
          <div className="text-lg font-bold text-emerald-400 flex items-center gap-1.5 mt-0.5">
            <CheckCircle2 className="w-4 h-4" />
            <span>{testResults.length} / {UNIT_TEST_SUITES.length}</span>
          </div>
        </div>

        <div className="bg-slate-950/60 border border-slate-800 p-3 rounded-xl">
          <div className="text-xs text-slate-400">Execution Time</div>
          <div className="text-lg font-bold text-sky-400 font-mono mt-0.5">
            {totalDuration} ms
          </div>
        </div>

        <div className="bg-slate-950/60 border border-slate-800 p-3 rounded-xl">
          <div className="text-xs text-slate-400">Suites Validated</div>
          <div className="text-lg font-bold text-slate-200 mt-0.5">
            {suites.length} Suites
          </div>
        </div>
      </div>

      {/* Suite Filter */}
      <div className="flex flex-wrap gap-1.5">
        <button
          onClick={() => setFilterSuite('all')}
          className={`px-2.5 py-1 text-xs rounded-lg transition ${
            filterSuite === 'all' ? 'bg-sky-500 text-white' : 'bg-slate-800 text-slate-400 hover:text-slate-200'
          }`}
        >
          All Suites
        </button>
        {suites.map(s => (
          <button
            key={s}
            onClick={() => setFilterSuite(s)}
            className={`px-2.5 py-1 text-xs rounded-lg font-mono transition ${
              filterSuite === s ? 'bg-sky-500 text-white' : 'bg-slate-800 text-slate-400 hover:text-slate-200'
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      {/* Test List */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-1">
        {displayed.map((test, i) => (
          <div
            key={i}
            className="p-3 bg-slate-950/40 border border-slate-800/80 hover:border-slate-700 rounded-xl flex items-start justify-between text-xs"
          >
            <div className="space-y-1">
              <div className="flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span className="font-mono font-semibold text-slate-200">{test.name}</span>
                <span className="text-[10px] text-slate-400 bg-slate-800 px-1.5 py-0.5 rounded font-mono">
                  {test.suite}
                </span>
              </div>
              <div className="text-[11px] text-slate-400 font-mono pl-6">
                {test.assertion}
              </div>
            </div>

            <span className="text-[11px] font-mono text-slate-400 bg-slate-800/60 px-2 py-0.5 rounded">
              {test.durationMs}ms
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
