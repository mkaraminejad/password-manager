import { VaultEntry, PasswordStrengthLevel, AuditSummary, AuditResult } from '../types';

export const INITIAL_ENTRIES: VaultEntry[] = [
  {
    id: 'entry-1',
    title: 'Google Workspace',
    username: 'alex.dev@gmail.com',
    password: 'G8#vX9$kL2!pW7zQ',
    url: 'https://accounts.google.com',
    notes: '2FA via hardware security key YubiKey 5C',
    folder: 'Work',
    tags: ['work', 'email', 'sso'],
    isFavorite: true,
    createdAt: Date.now() - 86400000 * 12,
    updatedAt: Date.now() - 86400000 * 2
  },
  {
    id: 'entry-2',
    title: 'GitHub Enterprise',
    username: 'alexdev-engineer',
    password: 'k9!P2#xL8$mZ4@vQ',
    url: 'https://github.com',
    notes: 'SSH key ed25519 generated on workstation',
    folder: 'Development',
    tags: ['dev', 'git', 'keys'],
    isFavorite: true,
    createdAt: Date.now() - 86400000 * 30,
    updatedAt: Date.now() - 86400000 * 5
  },
  {
    id: 'entry-3',
    title: 'Chase Bank Checking',
    username: 'alex_chase_99',
    password: 'ChasePassword123!',
    url: 'https://chase.com',
    notes: 'Primary checking and bills account',
    folder: 'Finance',
    tags: ['banking', 'finance'],
    isFavorite: true,
    createdAt: Date.now() - 86400000 * 60,
    updatedAt: Date.now() - 86400000 * 14
  },
  {
    id: 'entry-4',
    title: 'PayPal Personal',
    username: 'alex.dev@gmail.com',
    password: 'ChasePassword123!', // Reused on purpose for audit demonstration!
    url: 'https://paypal.com',
    notes: 'Linked to debit card',
    folder: 'Finance',
    tags: ['payments'],
    isFavorite: false,
    createdAt: Date.now() - 86400000 * 45,
    updatedAt: Date.now() - 86400000 * 10
  },
  {
    id: 'entry-5',
    title: 'Old Gaming Forum',
    username: 'alex_gamer',
    password: 'password123', // Weak on purpose for audit demonstration!
    url: 'https://classicgaming.net',
    notes: 'Created 2019',
    folder: 'Personal',
    tags: ['gaming', 'forum'],
    isFavorite: false,
    createdAt: Date.now() - 86400000 * 120,
    updatedAt: Date.now() - 86400000 * 90
  }
];

export function evaluatePassword(pwd: string): { level: PasswordStrengthLevel; score: number } {
  if (!pwd || pwd.length < 8) return { level: 'Very Weak', score: 1 };
  if (['password', '123456', 'password123', 'qwerty'].some(p => pwd.toLowerCase().includes(p))) {
    return { level: 'Weak', score: 2 };
  }
  let pool = 0;
  if (/[a-z]/.test(pwd)) pool += 26;
  if (/[A-Z]/.test(pwd)) pool += 26;
  if (/[0-9]/.test(pwd)) pool += 10;
  if (/[^a-zA-Z0-9]/.test(pwd)) pool += 32;

  const entropy = pwd.length * Math.log2(Math.max(1, pool));
  if (entropy < 36 || pwd.length < 10) return { level: 'Weak', score: 2 };
  if (entropy < 56 || pwd.length < 12) return { level: 'Fair', score: 3 };
  if (entropy < 75 || pwd.length < 16) return { level: 'Strong', score: 4 };
  return { level: 'Very Strong', score: 5 };
}

export function runVaultAudit(entries: VaultEntry[]): AuditSummary {
  const counts: Record<string, number> = {};
  for (const e of entries) {
    if (e.password) counts[e.password] = (counts[e.password] || 0) + 1;
  }

  let weakCount = 0;
  let reusedCount = 0;
  let strongCount = 0;

  const results: AuditResult[] = entries.map(entry => {
    const { level, score } = evaluatePassword(entry.password);
    const reuse = counts[entry.password] || 1;
    const isReused = reuse > 1;

    const issues: string[] = [];
    if (entry.password.length < 12) {
      issues.push(`Short password (${entry.password.length} chars, 16+ recommended)`);
    }
    if (score <= 2) {
      issues.push('Low entropy / predictable character structure');
    }
    if (isReused) {
      issues.push(`Reused across ${reuse} different accounts`);
      reusedCount++;
    }
    if (score <= 2) weakCount++;
    if (score >= 4) strongCount++;

    return {
      entryId: entry.id,
      title: entry.title,
      username: entry.username,
      strength: level,
      score,
      isReused,
      reuseCount: reuse,
      issues
    };
  });

  return {
    totalEntries: entries.length,
    weakCount,
    reusedCount,
    strongCount,
    results
  };
}
