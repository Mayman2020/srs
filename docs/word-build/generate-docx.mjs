/**
 * Builds two Arabic .docx deliverables from markdown sources under docs/.
 * Run: npm install && npm run build   (from this directory)
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  Table,
  TableRow,
  TableCell,
  WidthType,
} from 'docx';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const docsDir = path.join(__dirname, '..');
const outDir = path.join(docsDir, 'word-output');

const AR = /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/;

function stripLinks(s) {
  return s.replace(/\[([^\]]+)\]\([^)]*\)/g, '$1');
}

function runsFromLine(s) {
  const t = stripLinks(s).replace(/\u200f|\u200e/g, '').trimEnd();
  if (!t) return [new TextRun({ text: ' ' })];
  const parts = t.split(/\*\*/);
  return parts.map((p, i) => {
    const rtl = AR.test(p);
    return new TextRun({
      text: p,
      bold: i % 2 === 1,
      rightToLeft: rtl,
      font: rtl ? 'Arial' : 'Calibri',
    });
  });
}

function headingLevel(line) {
  // Longest # prefix first — otherwise `###` matches `####…`.
  if (line.startsWith('###### ')) return { level: HeadingLevel.HEADING_6, text: line.slice(7) };
  if (line.startsWith('##### ')) return { level: HeadingLevel.HEADING_5, text: line.slice(6) };
  if (line.startsWith('#### ')) return { level: HeadingLevel.HEADING_4, text: line.slice(5) };
  if (line.startsWith('### ')) return { level: HeadingLevel.HEADING_3, text: line.slice(4) };
  if (line.startsWith('## ')) return { level: HeadingLevel.HEADING_2, text: line.slice(3) };
  if (line.startsWith('# ')) return { level: HeadingLevel.HEADING_1, text: line.slice(2) };
  return null;
}

function isTableSep(line) {
  const t = line.trim();
  return /^\|?[\s|:-]+\|[\s|:-]+\|?$/.test(t) && t.includes('-');
}

function parseTableRow(line) {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((c) => c.trim());
}

function mdToParagraphs(md) {
  const lines = md.replace(/\r\n/g, '\n').split('\n');
  const children = [];
  let i = 0;

  while (i < lines.length) {
    const raw = lines[i];
    const line = raw.trimEnd();

    if (line.trim() === '' || line.trim() === '---') {
      i++;
      continue;
    }

    const h = headingLevel(line);
    if (h) {
      children.push(
        new Paragraph({
          bidirectional: true,
          heading: h.level,
          children: runsFromLine(h.text),
        }),
      );
      i++;
      continue;
    }

    if (line.startsWith('```')) {
      const code = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        code.push(lines[i]);
        i++;
      }
      if (i < lines.length) i++;
      children.push(
        new Paragraph({
          bidirectional: false,
          children: [
            new TextRun({
              text: code.join('\n') || ' ',
              font: 'Consolas',
              size: 20,
            }),
          ],
        }),
      );
      continue;
    }

    if (line.includes('|') && line.trim().startsWith('|')) {
      const rows = [];
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        const L = lines[i].trimEnd();
        if (!isTableSep(L)) rows.push(parseTableRow(L));
        i++;
      }
      if (rows.length > 0) {
        const colCount = Math.max(...rows.map((r) => r.length));
        const normalized = rows.map((r) => {
          const copy = [...r];
          while (copy.length < colCount) copy.push('');
          return copy;
        });
        children.push(
          new Table({
            width: { size: 100, type: WidthType.PERCENTAGE },
            rows: normalized.map(
              (cells) =>
                new TableRow({
                  children: cells.map(
                    (text) =>
                      new TableCell({
                        children: [
                          new Paragraph({
                            bidirectional: true,
                            children: runsFromLine(text || ' '),
                          }),
                        ],
                      }),
                  ),
                }),
            ),
          }),
        );
      }
      continue;
    }

    if (line.startsWith('- ') || line.startsWith('* ')) {
      children.push(
        new Paragraph({
          bidirectional: true,
          bullet: { level: 0 },
          children: runsFromLine(line.slice(2)),
        }),
      );
      i++;
      continue;
    }

    let block = line;
    let j = i + 1;
    while (
      j < lines.length &&
      lines[j].trim() !== '' &&
      !lines[j].trim().startsWith('#') &&
      !lines[j].trim().startsWith('- ') &&
      !lines[j].trim().startsWith('* ') &&
      !lines[j].trim().startsWith('|') &&
      !lines[j].trim().startsWith('```')
    ) {
      block += ' ' + lines[j].trim();
      j++;
    }
    children.push(
      new Paragraph({
        bidirectional: true,
        children: runsFromLine(block),
      }),
    );
    i = j;
  }

  return children;
}

function buildDoc(title, markdown) {
  return new Document({
    creator: 'SRS',
    title,
    description: title,
    sections: [
      {
        properties: {},
        children: mdToParagraphs(markdown),
      },
    ],
  });
}

async function writeDoc(filename, title, markdown) {
  const doc = buildDoc(title, markdown);
  const buf = await Packer.toBuffer(doc);
  const outPath = path.join(outDir, filename);
  fs.writeFileSync(outPath, buf);
  console.log('Wrote', outPath);
}

async function main() {
  fs.mkdirSync(outDir, { recursive: true });

  const business = fs.readFileSync(path.join(docsDir, 'business-logic-ar.md'), 'utf8');
  const demo = fs.readFileSync(path.join(docsDir, 'demo-users-ar.md'), 'utf8');
  const stories = fs.readFileSync(path.join(docsDir, 'word-input', 'user-stories-appendix-ar.md'), 'utf8');
  const technical = fs.readFileSync(path.join(docsDir, 'word-input', 'SRS-Technical-Full-Ar.md'), 'utf8');

  const doc1 =
    business +
    '\n\n# ملحق أ — مستخدمو البيئة التجريبية (من demo-users-ar)\n\n' +
    demo +
    '\n\n# ملحق ب — قصص المستخدم (User Stories)\n\n' +
    stories;

  await writeDoc(
    'SRS-Business-and-User-Stories-Ar.docx',
    'SRS — منطق العمل وقصص المستخدم',
    doc1,
  );
  await writeDoc('SRS-Technical-Documentation-Ar.docx', 'SRS — الوثيقة التقنية الكاملة', technical);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
