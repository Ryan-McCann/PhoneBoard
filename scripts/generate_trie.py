from wordfreq import top_n_list, zipf_frequency
import struct
from pathlib import Path
import unicodedata

def normalize_word(word):
    return (
        unicodedata.normalize("NFKD", word)
        .encode("ascii", "ignore")
        .decode("ascii")
        .lower()
    )

words = top_n_list("en", 80000)

entries = [
    (normalize_word(word), zipf_frequency(word, "en"))
    for word in words
]

entries = [
    (w, freq)
    for w, freq in entries
    if 2 <= len(w) <= 24
       and all(c.isalpha() or c in "-'" for c in w)
]

class TrieNode:
    def __init__(self):
        self.children = [None] * 28
        self.word = None
        self.frequency = 0
        self.id = 0

root = TrieNode()

def insert(node, entry):
    for c in entry[0]:
        if c == '\'':
            index = 26
        elif c == '-':
            index = 27
        else:
            index = ord(c) - ord('a')

        if node.children[index] is None:
            node.children[index] = TrieNode()

        node = node.children[index]

    node.word = entry[0]
    node.frequency = entry[1]

for entry in entries:
    insert(root, entry)

def assign_indexes(node, next_id=0):
    node.id = next_id
    next_id += 1
    for child in node.children:
        if child is not None:
            next_id = assign_indexes(child, next_id)

    return next_id

def index_char(i):
    if 0 <= i < 26:
        return chr(ord("a") + i)
    if i == 26:
        return "'"
    if i == 27:
        return "-"
    raise ValueError(i)

def fill_arrays(node):
    children = [
        (index_char(i), child)
        for i, child in enumerate(node.children)
        if child is not None
    ]

    first_child[node.id] = len(edge_char)
    child_count[node.id] = len(children)

    word_offset[node.id] = -1
    frequency[node.id] = int(node.frequency * 1000)

    if node.word is not None:
        word_offset[node.id] = len(word_bytes)
        word_bytes.extend(node.word.encode("utf-8"))
        word_bytes.append(0)

    for ch, child in children:
        edge_char.append(ch)
        edge_child.append(child.id)

    for ch, child in children:
        fill_arrays(child)

# Header:
#     magic
#     version
#     nodeCount
#     edgeCount
#     wordBytesLength
# Node arrays:
#     firstChild[nodeCount]
#     childCount[nodeCount]
#     wordOffset[nodeCount]
#     frequency[nodeCount]
# Edge arrays:
#     edgeChar[edgeCount]
#     edgeChild[edgeCount]
# Word data: (wordBytes)
#     [wordBytesLength]

node_count = assign_indexes(root)

first_child = [0] * node_count
child_count = [0] * node_count
word_offset = [-1] * node_count
frequency = [0] * node_count

edge_char = []
edge_child = []
word_bytes = bytearray()

fill_arrays(root)

edge_count = len(edge_child)
word_bytes_length = len(word_bytes)

out_path = Path("../app/src/main/assets/dicts/dict.trie")
out_path.parent.mkdir(parents=True, exist_ok=True)

with out_path.open("wb") as f:
    f.write(b"PBTR")
    f.write(struct.pack("<IIII", 1, node_count, edge_count, word_bytes_length))

    for arr in (first_child, child_count, word_offset, frequency):
        f.write(struct.pack(f"<{len(arr)}i", *arr))

    f.write(struct.pack(f"<{len(edge_char)}I", *(ord(c) for c in edge_char)))
    f.write(struct.pack(f"<{len(edge_child)}i", *edge_child))

    f.write(word_bytes)

print(f"entries: {len(entries)}")
print(f"nodes: {node_count}")
print(f"edges: {edge_count}")
print(f"word bytes: {word_bytes_length}")
print(f"wrote: {out_path}")

