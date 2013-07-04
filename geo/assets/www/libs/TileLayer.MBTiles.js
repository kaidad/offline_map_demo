// inspired by: https://github.com/coomsie/topomap.co.nz/blob/master/Resources/leaflet/TileLayer.DB.js
L.TileLayer.MBTiles = L.TileLayer.extend({
    //db: SQLitePlugin
    mbTilesDB: null,

    initialize: function (url, options, db) {
        this.mbTilesDB = db;

        L.Util.setOptions(this, options);
    },
    getTileUrl: function (tile, tilePoint) {
        var z = tilePoint.z;
        var x = tilePoint.x;
        var y = tilePoint.y;
        var base64Prefix = 'data:image/gif;base64,';

        console.log('getTileUrl: querying database with (x,y,z): ' + x + ', ' + y + ', ' + z);
        this.mbTilesDB.transaction(function (tx) {
            tx.executeSql("SELECT tile_data FROM images INNER JOIN map ON images.tile_id = map.tile_id WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?", [z, x, y], function (tx, res) {
                if (!res || !res.rows || res.rows.length != 1) {
                    console.log('Expected tile query to return exactly one row, but returned: ' + (res && res.rows && res.rows.length));
                } else {
                    tile.src = base64Prefix + res.rows.item(0).tile_data;
                }
            }, function (er) {
                console.log('Error occurred while querying tile db: ', er);
            });
        });
    },
    _loadTile: function (tile, tilePoint) {
        tile._layer = this, tile.onload = this._tileOnLoad, tile.onerror = this._tileOnError, this._adjustTilePoint(tilePoint), this.getTileUrl(tile, tilePoint)
    }
});