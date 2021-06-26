# Scripts

[国土数値情報 高速道路時系列データ](https://nlftp.mlit.go.jp/ksj/gml/datalist/KsjTmplt-N06-v1_2.html)から動作に必要なJSONファイルを作成する函数群．

## 各ファイルの説明
### generate_ic_json.py
* N06-19_Joint.geojsonから全ICの座標が記載されたJSONファイルを作成するスクリプト
* 作成されるJSONファイル名はinterchanges.json

### highways_graph.json
* 各ICおよびJCT間の隣接関係をグラフにしたJSONファイル
* 何かしらの方法で作られた
* ただし全国に同一名のICがある場合，それらのノードが同一ノードになってしまってゐる．例へば郡山ICは東北道と近畿道の2道に存在するが，東北道では郡山JCTと隣接し，近畿道では天理ICと隣接してゐる．このため，郡山JCTと天理ICの間にワームホールのやうなものが存在することになってしまふ．

### remove_duplication.py
* 上記の重複問題を解決するために作られたスクリプト
* 作成されるJSONファイル名はnew_graph.json

### generate_joint_graph.py
* new_graph.jsonに距離情報を付加するスクリプト
* 作成されるJSONファイル名はjoints_graph.json


## 使ひ方
1. 国土数値情報 高速道路時系列データをダウンロード
2. ダウンロードしたファイルを解凍し，N06-19_Joint.geojsonをこのディレクトリ上に配置
4. highways_graph.jsonを準備
5. generate_ic_json.py, remove_duplication.py, generate_joint_graph.pyの順に実行
