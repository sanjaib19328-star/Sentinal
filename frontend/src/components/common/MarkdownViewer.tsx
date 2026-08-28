import React, { useState } from 'react';
import { Copy, Check } from 'lucide-react';

interface MarkdownViewerProps {
  content: string;
}

export const MarkdownViewer: React.FC<MarkdownViewerProps> = ({ content }) => {
  if (!content) return null;

  const renderInline = (text: string): React.ReactNode => {
    // Split by code blocks first
    const parts: React.ReactNode[] = [];
    const inlineCodeRegex = /`([^`]+)`/g;
    let lastIndex = 0;
    let match;

    while ((match = inlineCodeRegex.exec(text)) !== null) {
      if (match.index > lastIndex) {
        parts.push(renderBoldAndText(text.substring(lastIndex, match.index), `txt-${lastIndex}`));
      }
      const codeVal = match[1];
      // Check if it is an HTTP method
      if (/^(GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD)$/i.test(codeVal)) {
        const m = codeVal.toUpperCase();
        let cls = 'method-pill-other';
        if (m === 'GET') cls = 'method-pill-get';
        else if (m === 'POST') cls = 'method-pill-post';
        else if (m === 'PUT' || m === 'PATCH') cls = 'method-pill-put';
        else if (m === 'DELETE') cls = 'method-pill-delete';
        parts.push(
          <span key={`code-${match.index}`} className={`method-pill ${cls}`}>
            {m}
          </span>
        );
      } else {
        parts.push(<code key={`code-${match.index}`}>{codeVal}</code>);
      }
      lastIndex = inlineCodeRegex.lastIndex;
    }

    if (lastIndex < text.length) {
      parts.push(renderBoldAndText(text.substring(lastIndex), `txt-${lastIndex}`));
    }

    return parts;
  };

  const renderBoldAndText = (text: string, keyPrefix: string): React.ReactNode => {
    const boldRegex = /\*\*([^*]+)\*\*/g;
    const parts: React.ReactNode[] = [];
    let lastIdx = 0;
    let match;

    while ((match = boldRegex.exec(text)) !== null) {
      if (match.index > lastIdx) {
        parts.push(text.substring(lastIdx, match.index));
      }
      parts.push(<strong key={`${keyPrefix}-b-${match.index}`}>{match[1]}</strong>);
      lastIdx = boldRegex.lastIndex;
    }

    if (lastIdx < text.length) {
      parts.push(text.substring(lastIdx));
    }

    return <React.Fragment key={keyPrefix}>{parts}</React.Fragment>;
  };

  // Block parser
  const lines = content.split('\n');
  const blocks: React.ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // 1. Code Block ```
    if (line.trim().startsWith('```')) {
      const lang = line.trim().replace(/^```/, '').trim();
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // Skip closing ```
      const fullCode = codeLines.join('\n');
      blocks.push(<CodeBlockViewer key={`code-blk-${i}`} code={fullCode} language={lang} />);
      continue;
    }

    // 2. Table | col1 | col2 |
    if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
      const tableLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
        tableLines.push(lines[i].trim());
        i++;
      }

      if (tableLines.length >= 2) {
        const headerCols = tableLines[0]
          .split('|')
          .slice(1, -1)
          .map((c) => c.trim());
        const dataRows = tableLines.slice(2); // skip separator row like | :--- | :--- |

        blocks.push(
          <div key={`table-${i}`} className="markdown-table-wrapper">
            <table className="markdown-table">
              <thead>
                <tr>
                  {headerCols.map((col, idx) => (
                    <th key={`th-${idx}`}>{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {dataRows.map((rowStr, rowIdx) => {
                  const cells = rowStr
                    .split('|')
                    .slice(1, -1)
                    .map((c) => c.trim());
                  return (
                    <tr key={`tr-${rowIdx}`}>
                      {cells.map((cell, cellIdx) => (
                        <td key={`td-${rowIdx}-${cellIdx}`}>{renderInline(cell)}</td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        );
        continue;
      }
    }

    // 3. Headers ###, ####, ##, #
    if (line.startsWith('#### ')) {
      blocks.push(<h4 key={`h4-${i}`}>{renderInline(line.substring(5))}</h4>);
      i++;
      continue;
    }
    if (line.startsWith('### ')) {
      blocks.push(<h3 key={`h3-${i}`}>{renderInline(line.substring(4))}</h3>);
      i++;
      continue;
    }
    if (line.startsWith('## ')) {
      blocks.push(<h3 key={`h2-${i}`}>{renderInline(line.substring(3))}</h3>);
      i++;
      continue;
    }
    if (line.startsWith('# ')) {
      blocks.push(<h3 key={`h1-${i}`}>{renderInline(line.substring(2))}</h3>);
      i++;
      continue;
    }

    // 4. Blockquote > ...
    if (line.startsWith('> ')) {
      const quoteLines: string[] = [];
      while (i < lines.length && lines[i].startsWith('> ')) {
        quoteLines.push(lines[i].substring(2));
        i++;
      }
      blocks.push(
        <blockquote key={`quote-${i}`}>
          {quoteLines.map((ql, qIdx) => (
            <p key={`qp-${qIdx}`}>{renderInline(ql)}</p>
          ))}
        </blockquote>
      );
      continue;
    }

    // 5. Unordered list item - ... or * ...
    if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
      const listItems: string[] = [];
      while (i < lines.length && (lines[i].trim().startsWith('- ') || lines[i].trim().startsWith('* '))) {
        listItems.push(lines[i].trim().substring(2));
        i++;
      }
      blocks.push(
        <ul key={`ul-${i}`}>
          {listItems.map((item, idx) => (
            <li key={`li-${idx}`}>{renderInline(item)}</li>
          ))}
        </ul>
      );
      continue;
    }

    // 6. Regular paragraph or empty line
    if (line.trim().length > 0) {
      blocks.push(<p key={`p-${i}`}>{renderInline(line)}</p>);
    }

    i++;
  }

  return <div className="markdown-content">{blocks}</div>;
};

const CodeBlockViewer: React.FC<{ code: string; language?: string }> = ({ code, language }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div style={{ position: 'relative', margin: '0.75rem 0' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: '#1e293b',
          color: '#94a3b8',
          fontSize: '0.6875rem',
          fontWeight: 600,
          padding: '0.375rem 0.875rem',
          borderTopLeftRadius: 'var(--radius-md)',
          borderTopRightRadius: 'var(--radius-md)',
          borderBottom: '1px solid #334155',
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
        }}
      >
        <span>{language || 'code'}</span>
        <button
          type="button"
          onClick={handleCopy}
          style={{
            background: 'transparent',
            border: 'none',
            color: copied ? '#10b981' : '#94a3b8',
            cursor: 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.25rem',
            fontSize: '0.6875rem',
          }}
        >
          {copied ? <Check style={{ width: '0.75rem', height: '0.75rem' }} /> : <Copy style={{ width: '0.75rem', height: '0.75rem' }} />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      <pre style={{ margin: 0, borderTopLeftRadius: 0, borderTopRightRadius: 0 }}>
        <code>{code}</code>
      </pre>
    </div>
  );
};
