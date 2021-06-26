import json
from typing import Any
import itertools

def generate_joint_list(geojson_path: str) -> Any:
    """国土数値情報 高速道路時系列データからインターチェンジ・JCTの一覧を作成する函数
    次のようなListをreturnする
    [
        {"name": "南相馬鹿島SIC", "point": [ 140.91991648000001, 37.71533259 ]},
        {"name": "高岡砺波SIC", "point": [ 137.00070762, 36.65547594 ]}, ...
    ]

    Args:
        geojson_path (str): 国土数値情報 高速道路時系列データのgeojsonファイルのパス
    """

    with open(geojson_path) as f:
        features = json.load(f)['features']

    ic = filter(lambda x: x['properties']['接合部種別'] in ['1', '2', '3'], features)

    return list(map(lambda x: {'name': x['properties']['地点名'], 'point': x['geometry']['coordinates']}, ic))


def search_distance(name1: str, name2: str, **kwargs) -> float:
    """(隣接する)2つのジョイント間の距離を求める函数
    この函数を改善することでよりよいグラフが得られるはず・・・

    Args:
        name1 (str): ジョイント名
        name2 (str): ジョイント名
    """
    dist = lambda x1, y1, x2, y2: ((x1 - x2) ** 2 + (y1 - y2) ** 2) ** 1/2

    joints = kwargs['joints']
    a = list(filter(lambda x: name1.startswith(x['name']), joints))
    b = list(filter(lambda x: name2.startswith(x['name']), joints))

    mindist = 999999.9

    for ic1, ic2 in itertools.product(a, b):
        x1, y1 = ic1['point']
        x2, y2 = ic2['point']

        mindist = min(mindist, dist(x1, y1, x2, y2))

    return mindist


def generate_joint_graph(
    geojson_path: str, graph_path: str
) -> None:
    joints = generate_joint_list(geojson_path)

    result_graph: Any = {}

    with open(graph_path) as f:
        org_graph = json.load(f)

    for k, adj_joints in org_graph.items():
        res_value = []
        for v in adj_joints:
            dist = search_distance(k, v['name'], joints=joints)

            res_value.append(dict(v, **{'distance': dist}))

        result_graph[k] = res_value

    with open('dist/joints_graph.json', 'w') as f:
        json.dump(result_graph, f, indent=4, ensure_ascii=False)


if __name__ == '__main__':
    generate_joint_graph('N06-19_Joint.geojson', 'dist/new_graph.json')
