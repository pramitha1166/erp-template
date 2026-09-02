import { describe, expect, it } from "vitest";

import { flattenTree } from "./tree";

interface Node {
  id: string;
  parentId: string | null;
  name: string;
}

describe("flattenTree", () => {
  it("orders children depth-first directly under their parent", () => {
    const items: Node[] = [
      { id: "1100", parentId: "1000", name: "Current Assets" },
      { id: "1000", parentId: null, name: "Assets" },
      { id: "1110", parentId: "1100", name: "Cash" },
      { id: "2000", parentId: null, name: "Liabilities" },
    ];

    const rows = flattenTree(items);

    expect(rows.map((row) => row.item.id)).toEqual(["1000", "1100", "1110", "2000"]);
  });

  it("assigns depth by nesting level", () => {
    const items: Node[] = [
      { id: "1000", parentId: null, name: "Assets" },
      { id: "1100", parentId: "1000", name: "Current Assets" },
      { id: "1110", parentId: "1100", name: "Cash" },
    ];

    const rows = flattenTree(items);

    expect(rows.map((row) => row.depth)).toEqual([0, 1, 2]);
  });

  it("treats a node whose parent is missing from the list as a root instead of dropping it", () => {
    const items: Node[] = [{ id: "1110", parentId: "missing-parent", name: "Cash" }];

    const rows = flattenTree(items);

    expect(rows).toHaveLength(1);
    expect(rows[0].depth).toBe(0);
  });
});
