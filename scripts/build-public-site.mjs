import { constants, copyFileSync, lstatSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = join(root, 'vercel-site-output');
const publicFiles = [
  'index.html',
  'greater-art-user-demo.html',
  'greater-art-technical-demo.html',
  'greater-art/releases/GreaterArt-1.21.93.apk',
  'greater-art/releases/GreaterArt-1.21.39.apk',
];

// The production website publishes these individual files from the checkout.
for (const name of publicFiles) {
  const parts = name.split('/');
  let source = root;
  for (const [index, part] of parts.entries()) {
    source = join(source, part);
    const attributes = lstatSync(source);
    const expectedType = index === parts.length - 1 ? attributes.isFile() : attributes.isDirectory();
    if (!expectedType) throw Error(`Public input must be a regular file in ordinary directories: ${name}`);
  }
}

// Require a fresh destination so a previous build cannot widen the output.
mkdirSync(output);
for (const name of publicFiles) {
  const target = join(output, name);
  mkdirSync(dirname(target), { recursive: true });
  copyFileSync(join(root, name), target, constants.COPYFILE_EXCL);
}

console.log(`Prepared ${publicFiles.length} allowlisted site files.`);
