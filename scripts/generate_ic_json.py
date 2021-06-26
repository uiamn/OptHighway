import json

def generate_ic_json(geojson_path: str) -> None:
    """国土数値情報 高速道路時系列データからインターチェンジの一覧を作成する函数
    次のようなjsonをdist/ic.jsonに出力する
    [
        {"name": "南相馬鹿島SIC", "point": [ 140.91991648000001, 37.71533259 ]},
        {"name": "高岡砺波SIC", "point": [ 137.00070762, 36.65547594 ]}, ...
    ]

    Args:
        geojson_path (str): 国土数値情報 高速道路時系列データのgeojsonファイルのパス
    """

    with open(geojson_path) as f:
        features = json.load(f)['features']

    ic = filter(lambda x: x['properties']['接合部種別'] in ['1', '2'], features)
    j = map(lambda x: {'name': x['properties']['地点名'], 'point': x['geometry']['coordinates']}, ic)

    with open('dist/interchanges.json', 'w') as f:
        json.dump(list(j), f, indent=4, ensure_ascii=False)


if __name__ == '__main__':
    generate_ic_json('N06-19_Joint.geojson')
