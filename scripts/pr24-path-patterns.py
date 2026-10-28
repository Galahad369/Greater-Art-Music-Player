"""Path patterns for Python replacement and Git ERE scanning.

These engines have different escape rules. Never pass Python's raw \\r/\\n
escapes to Git ERE, or permit a Python replacement to consume another line.
"""

PATTERNS = [
    r'[A-Za-z]:[\\/]+[Uu][Ss][Ee][Rr][Ss][\\/]+[^\\/\r\n]+[\\/]',
    r'/[Uu]sers/[^/\r\n]+/',
    '/' + r'home/[^/\r\n]+/',
]

# git grep scans one line at a time. POSIX control classes also reject CR in
# CRLF files without interpreting an account's letters r or n as separators.
GREP_PATTERNS = [
    r'[A-Za-z]:[\\/]+[Uu][Ss][Ee][Rr][Ss][\\/]+[^\\/[:cntrl:]]+[\\/]',
    r'/[Uu]sers/[^/[:cntrl:]]+/',
    '/' + r'home/[^/[:cntrl:]]+/',
]
GREP_PATTERN = '(' + '|'.join(GREP_PATTERNS) + ')'
