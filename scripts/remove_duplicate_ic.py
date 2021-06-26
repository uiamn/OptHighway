import json
from pprint import pprint

from generate_joint_graph import generate_joint_list

dist = lambda x1, y1, x2, y2: ((x1 - x2) ** 2 + (y1 - y2) ** 2 ) ** 1/2


def get_candidate(prefix, new_graph):
    candidate = {}
    for k, v in new_graph.items():
        if k.startswith(prefix + '('):
            candidate[k] = v

    return candidate


def remove_duplicate_ic() -> None:
    joints = generate_joint_list('N06-19_Joint.geojson')

    with open('dist/new_graph.json') as f:
        graph = json.load(f)

    po = []
    duplication = []
    # 重複してゐるIC名のリストを作る
    for ic in joints:
        if (ic_name := ic['name']) in po:
            duplication.append(ic_name)
        else:
            po.append(ic_name)

    duplication = list(set(duplication))

    true_duplication = []
    new_ic_part = []

    for ic in joints:
        ic_name = ic['name']
        if ic_name not in duplication:
            continue
        candidate = get_candidate(ic_name, graph)

        if candidate == {}:
            continue

        # 真の重複
        true_duplication.append(ic_name)
        x1, y1 = ic['point']

        # candidateに含まれるICのうち，隣接するICとの距離の平均が小さい方が恐らく正しい
        true_ic_name = None
        min_ic_dist = 99999.9

        for k, adj_ics in candidate.items():
            cnt = 0
            tmp_dist = 0

            pprint(adj_ics)
            for adj in adj_ics:
                a = list(filter(lambda x: x['name'] == adj['name'], joints))
                if len(a) == 0:
                    continue

                x2, y2 = a[0]['point']

                tmp_dist += dist(x1, y1, x2, y2)
                cnt += 1

            if (d := tmp_dist / cnt) < min_ic_dist:
                true_ic_name = k
                min_ic_dist = d

        new_ic_part.append({
            'name': true_ic_name, 'point': ic['point']
        })


    with open('dist/interchanges.json') as f:
        old_ics = json.load(f)

    final_ics = list(filter(lambda x: x['name'] not in duplication, old_ics)) + new_ic_part

    with open('dist/final_interchanges.json', 'w') as f:
        json.dump(final_ics, f, indent=4, ensure_ascii=False)

if __name__ == '__main__':
    remove_duplicate_ic()
