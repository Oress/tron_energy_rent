import { rm } from 'node:fs/promises';
import { resolve } from 'node:path';

const generatedApiDirectory = resolve('src/app/core/api');

if (!generatedApiDirectory.endsWith(resolve('src/app/core/api'))) {
  throw new Error(`Refusing to clean unexpected path: ${generatedApiDirectory}`);
}

await rm(generatedApiDirectory, { recursive: true, force: true });
