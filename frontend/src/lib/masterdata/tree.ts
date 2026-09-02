export interface TreeNode {
  id: string;
  parentId: string | null;
}

export interface TreeRow<T> {
  item: T;
  depth: number;
}

/**
 * MDM-3/4/6: flattens a parent-linked hierarchy (Chart of Accounts, Cost Centres, Item Groups all share this shape)
 * into depth-first, indent-ready rows. A node whose parent isn't in `items` (shouldn't happen, but a stale fetch
 * mid-edit could show it transiently) is treated as a root rather than dropped, so nothing silently disappears.
 */
export function flattenTree<T extends TreeNode>(items: T[]): TreeRow<T>[] {
  const byParent = new Map<string | null, T[]>();
  const ids = new Set(items.map((item) => item.id));
  for (const item of items) {
    const parentKey = item.parentId !== null && ids.has(item.parentId) ? item.parentId : null;
    const siblings = byParent.get(parentKey) ?? [];
    siblings.push(item);
    byParent.set(parentKey, siblings);
  }

  const rows: TreeRow<T>[] = [];
  function visit(parentKey: string | null, depth: number) {
    for (const item of byParent.get(parentKey) ?? []) {
      rows.push({ item, depth });
      visit(item.id, depth + 1);
    }
  }
  visit(null, 0);
  return rows;
}
