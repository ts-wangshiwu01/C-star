# mem0 Local Bulk Memory Cleanup

When mem0 local memories accumulate (100+ entries) and need pruning, the
mem0_local_search/mem0_local_delete tools are too slow for bulk operations.
Use Qdrant HTTP API directly.

## Step 1: List all memories with IDs

```bash
curl -s "http://localhost:6333/collections/memories/points/scroll" \
  -H "Content-Type: application/json" \
  -d '{"limit": 200, "with_payload": true}' | python3 -c "
import sys, json
data = json.load(sys.stdin)
points = data.get('result', {}).get('points', [])
for p in points:
    mem = p['payload'].get('memory', '')
    print(f'{p[\"id\"]}|||{mem[:200]}')
" > /tmp/all_memories.txt
```

## Step 2: Classify — what to keep vs delete

Keep only **stable, durable facts**:
- User identity & preferences (name, role, privacy rules, style preferences)
- Environment facts (OS, container runtime, key paths)
- Project architecture (stable structural facts, not task progress)
- Key decisions (durable decisions, not daily state)
- Tool/skill configuration (how tools are set up)

Delete:
- **Duplicates** — same fact stored 2+ times
- **Completed events** — "created X document", "deleted Y skill"
- **Fleeting values** — specific fund amounts, page counts, token usage
- **Superseded proposals** — approaches the user rejected
- **Covered by wiki** — content that now lives in wiki pages
- **Stale paths** — old directory references
- **Resolved issues** — problems that were fixed

Target: compress 200+ → 25-35 high-signal memories.

## Step 3: Bulk delete via Qdrant API

```bash
# Delete specific point IDs in batches of 20
curl -s -X POST "http://localhost:6333/collections/memories/points/delete" \
  -H "Content-Type: application/json" \
  -d '{"points": ["<uuid1>", "<uuid2>", ...]}'
```

**Important**: mem0 stores multiple vector points per memory. After deleting
by ID, scroll again to check for remaining points and repeat if needed.

## Step 4: Re-add rescued facts

After bulk delete, use `mem0_local_conclude` to re-add any important stable
facts that were lost (e.g., user identity, project architecture, wiki rules).

## Step 5: Verify

```bash
curl -s "http://localhost:6333/collections/memories" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'Total memories: {data[\"result\"][\"points_count\"]}')
"
```

Also run `mem0_local_profile` to visually inspect what remains.
