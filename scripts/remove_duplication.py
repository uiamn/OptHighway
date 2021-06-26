"""重複を取り除くために色々頑張ったスクリプト
"""

import json
import copy
from pprint import pprint

dist = lambda x1, y1, x2, y2: ((x1*2 - x2*2) ** 2 + (y1 - y2) ** 2) ** 1/2

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
        if (d := dist(x1, y1, x2, y2)) > 1:
            candidate.append(v['name'])
            print(v['name'])
            print(d)
    else:
        a.append(v['name'])
        coord[v['name']] = v['point']

candidate = list(set(candidate))
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

pprint(new_nodes)
for new_name, adj_joints in new_nodes.items():
    old_name = new_name.split('(')[0]
    for adj_joint in adj_joints:
        joint_name = adj_joint['name']
        for i, joint in enumerate(graph[joint_name]):
            if joint['name'] == old_name:
                graph[joint_name][i] = {'name': new_name, 'road': joint['road']}

for new_name, adj_joints in new_nodes.items():
    for adj_joint in adj_joints:
        if adj_joint['name'] in candidate:
            adj_joint['name'] = f"{adj_joint['name']}({adj_joint['road']})"

# pprint(graph)

# # new_graph = copy.deepcopy(graph)
# new_graph = graph

# グラフに新しく作ったノードを追加し，重複は消す
graph = dict(graph, **new_nodes)

for c in candidate:
    graph.pop(c)

with open('dist/new_graph.json', 'w') as f:
    json.dump(graph, f, indent=4, ensure_ascii=False)


# pprint(new_nodes)

# # 接続関係を更新
# for ic_name, adj_nodes in new_nodes.items():
#     """
#         ic_name: 新しい(正しい)ICの名前
#         adj_nodes: ICに隣接するJointたち

#         隣接するjointに含まれる古いIC名を新しいIC名(ic_name)に置き換へればよい
#         ただし，jointの名前がそもそも変はってゐる場合もあるので注意
#     """

#     old_name = ic_name.split('(')[0]

#     for adj in adj_nodes:
#         adj_name = adj['name']
#         if adj_name in candidate:
#             print(ic_name, adj_name)
#             continue

#         if adj['name'] in candidate:
#             adj_name = f"{adj_name}({adj['road']})"

#         # 元々のグラフのadjに含まれる旧名のICを新名に置き換ればよい
#         adj_adj_ics = graph[adj['name']]

#         for i, ic in enumerate(adj_adj_ics):
#             pprint(ic)
#             if ic['name'] == old_name:
#                 adj_adj_ics.pop(i)
#                 adj_adj_ics.append({'name': ic_name, 'road': ic['road']})


# with open('dist/new_graph.json', 'w') as f:
#     json.dump(new_graph, f, indent=4, ensure_ascii=False)
