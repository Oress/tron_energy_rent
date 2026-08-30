import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const [, , version = 'development', output = 'src/environments/version.ts', commit = 'local'] = process.argv;
const target = resolve(output);
const generatedAt = new Date().toISOString();

await mkdir(dirname(target), { recursive: true });
await writeFile(
  target,
  `export const versionInfo = ${JSON.stringify({ version, commit, generatedAt }, null, 2)} as const;\n`,
  'utf8',
);
