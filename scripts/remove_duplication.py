"""重複を取り除くために色々頑張ったスクリプト
"""

import json
from pprint import pprint

dist = lambda x1, y1, x2, y2: ((x1 - x2) ** 2 + (y1 - y2) ** 2) ** 1/2

with open('dist/interchanges.json') as f:
    ics = json.load(f)

with open('highways_graph.json') as f:
    graph = json.load(f)


a = []
coord = {}
candidate = []
for v in ics:
    if v['name'] in a:
        x1, y1 = v['point']
        x2, y2 = coord[v['name']]
        if (d := dist(x1, y1, x2, y2)) > 0.1:
            candidate.append(v['name'])
    else:
        a.append(v['name'])
        coord[v['name']] = v['point']

candidate = set(candidate)
print(candidate)

new_nodes = {}

for c in candidate:
    # print(graph[c])
    adj_ics = graph[c]

    # 道路毎にICを複製する
    po = {}
    for ic in adj_ics:
        if (r := ic['road']) not in po:
            po[r] = [ic]
        else:
            po[r].append(ic)

    for road_name, ics in po.items():
        new_nodes[f"{c}({road_name})"] = ics

new_graph = graph

# グラフに新しく作ったノードを追加
new_graph = dict(new_graph, **new_nodes)

# 接続関係を更新
for ic_name, adj_nodes in new_nodes.items():
    # ic_name: 新しい(正しい)ICの名前
    # adj_nodes: ICに隣接するJointたち
    old_name = ic_name.split('(')[0]

    for adj in adj_nodes:
        adj_name = adj['name']

        if adj['name'] in candidate:
            adj_name = f"{adj_name}({adj['road']})"

        # 元々のグラフのadjに含まれる旧名のICを新名に置き換ればよい
        adj_adj_ics = graph[adj['name']]
        po = []
        for ic in adj_adj_ics:
            if ic['name'] == old_name:
                po.append({'name': ic_name, 'road': ic['road']})
            else:
                po.append(ic)

        new_graph[adj_name] = po


# 元々のグラフから複数あるICたちを消す
for c in candidate:
    new_graph.pop(c)



with open('dist/new_graph.json', 'w') as f:
    json.dump(new_graph, f, indent=4, ensure_ascii=False)
