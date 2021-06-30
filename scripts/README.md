# Scripts

[国土数値情報 高速道路時系列データ](https://nlftp.mlit.go.jp/ksj/gml/datalist/KsjTmplt-N06-v1_2.html)から動作に必要なJSONファイルを作成する函数群．

## 各ファイルの説明
### generate_ic_json.py
* N06-19_Joint.geojsonから全ICの座標が記載されたJSONファイルを作成するスクリプト
* 作成されるJSONファイル名はinterchanges.json
* これにもIC名の重複が含まれる

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

### remove_duplicate_ic.py
* interchanges.jsonの重複を取り除くスクリプト
* 作成されるJSONファイル名はfinal_interchanges.json

## 使ひ方
1. 国土数値情報 高速道路時系列データをダウンロード
2. ダウンロードしたファイルを解凍し，N06-19_Joint.geojsonをこのディレクトリ上に配置
4. highways_graph.jsonを準備
5. generate_ic_json.py, remove_duplication.py, remove_duplicate_ic.py, generate_joint_graph.py の順に実行
6. generate_joint_graph.py実行時に表示されるICたちは上手く重複除去ができてゐない恐れがある．ので，手動でfinal_interchanges.jsonを修正する(最悪)．確認してゐるものは以下
   * 沼田: 沼田(深川沼田道路)，沼田(沼田幌糠道路)がどちらか一方になってしまふので，コピィする．
   * 福岡: 福岡(福岡高速4号線)が2個でき，福岡(九州縦貫自動車道鹿児島線)がない．場所はほぼ同一なのでどちらか一方を変更する．
   * 大野: 大野(広島岩国道路)が2個でき，大野(永平寺大野道路)がない．経度が135度を超えてゐる方を永平寺大野道路に変更する．
7. joints_graph.jsonに東海JCTは東京と愛知にあり重複してゐる．手動で直す．
