/**
 * Keyword and snippet completions for the two languages Monaco has no language
 * service for.
 *
 * <p>`${1:name}` and `$0` are Monaco snippet placeholders: tab stops, with `$0`
 * where the cursor ends up. The lists cover the vocabulary that actually shows
 * up in an algorithm answer — the collection types and loop shapes — rather
 * than every keyword in the language.
 */

export type Completion = {
  label: string;
  /** Defaults to `label`. */
  insertText?: string;
  /** Right-hand hint in the completion list. */
  detail?: string;
  /** True when `insertText` uses snippet placeholders. */
  snippet?: boolean;
};

const keywords = (detail: string, ...labels: string[]): Completion[] =>
  labels.map((label) => ({ label, detail }));

export const JAVA_COMPLETIONS: Completion[] = [
  ...keywords(
    'keyword',
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'continue',
    'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for',
    'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'new', 'null', 'private',
    'protected', 'public', 'return', 'short', 'static', 'super', 'switch', 'this', 'throw', 'throws',
    'try', 'void', 'while', 'true', 'false', 'var',
  ),
  ...keywords(
    'java.util',
    'ArrayList', 'Arrays', 'ArrayDeque', 'Collections', 'Comparator', 'Deque', 'HashMap', 'HashSet',
    'Integer', 'Iterator', 'LinkedList', 'List', 'Long', 'Map', 'Math', 'Optional', 'PriorityQueue',
    'Queue', 'Set', 'Stack', 'String', 'StringBuilder', 'TreeMap', 'TreeSet',
  ),
  { label: 'sout', insertText: 'System.out.println($0);', detail: 'print to stdout', snippet: true },
  { label: 'fori', insertText: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n    $0\n}', detail: 'index loop', snippet: true },
  { label: 'foreach', insertText: 'for (${1:var} ${2:item} : ${3:items}) {\n    $0\n}', detail: 'enhanced for', snippet: true },
  { label: 'map', insertText: 'Map<${1:Integer}, ${2:Integer}> ${3:map} = new HashMap<>();$0', detail: 'new HashMap', snippet: true },
  { label: 'list', insertText: 'List<${1:Integer}> ${2:list} = new ArrayList<>();$0', detail: 'new ArrayList', snippet: true },
];

export const PYTHON_COMPLETIONS: Completion[] = [
  ...keywords(
    'keyword',
    'and', 'as', 'assert', 'break', 'class', 'continue', 'def', 'del', 'elif', 'else',
    'except', 'finally', 'for', 'from', 'global', 'if', 'import', 'in', 'is', 'lambda',
    'None', 'nonlocal', 'not', 'or', 'pass', 'raise', 'return', 'try', 'while', 'with',
    'yield', 'True', 'False', 'self',
  ),
  ...keywords(
    'builtin',
    'abs', 'all', 'any', 'bin', 'bool', 'dict', 'divmod', 'enumerate', 'filter', 'float',
    'int', 'len', 'list', 'map', 'max', 'min', 'range', 'reversed', 'round', 'set',
    'sorted', 'str', 'sum', 'tuple', 'zip',
  ),
  ...keywords('collections / heapq', 'Counter', 'deque', 'heappush', 'heappop', 'heapify'),
  { label: 'forr', insertText: 'for ${1:i} in range(${2:n}):\n    $0', detail: 'range loop', snippet: true },
  { label: 'fore', insertText: 'for ${1:item} in ${2:items}:\n    $0', detail: 'iterate', snippet: true },
  { label: 'enum', insertText: 'for ${1:i}, ${2:value} in enumerate(${3:items}):\n    $0', detail: 'enumerate loop', snippet: true },
  { label: 'defaultdict', insertText: 'defaultdict(${1:int})$0', detail: 'collections.defaultdict', snippet: true },
];
