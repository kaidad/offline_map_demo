/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var localFileName;	// the filename of the local mbtiles file
var remoteFile;		// the url of the remote mbtiles file to be downloaded
var msg;			// the span to show messages

localFileName = 'test.mbtiles';
//remoteFile = 'http://dl.dropbox.com/u/14814828/OSMBrightSLValley.mbtiles';
//remoteFile = 'http://192.168.0.4:8080/open-streets-dc_ee45cf.mbtiles';
remoteFile = 'http://192.168.0.4:8080/OSMBrightSLValley.mbtiles';

var app = {
    // Application Constructor
    initialize: function () {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function () {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicity call 'app.receivedEvent(...);'
    onDeviceReady: function () {
        app.receivedEvent('deviceready');
    },

    // Update DOM on a Received Event
    receivedEvent: function (id) {
        var parentElement = document.getElementById(id);
//        var listeningElement = parentElement.querySelector('.listening');
//        var receivedElement = parentElement.querySelector('.received');
//
//        listeningElement.setAttribute('style', 'display:none;');
//        receivedElement.setAttribute('style', 'display:block;');

        msg = document.getElementById('message');

        msg.innerHTML = 'Building map...';

        var that = this;
        window.sqlitePlugin.databaseExists(localFileName, function (retArray) {
            if (retArray[0]) {
                console.log('Database file [' + localFileName + '] already exists, building map');
                that.buildMap();
            } else {
                // file does not exist
                window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
                    fs = fileSystem;

                    //need to download the db file to the same place SQLite plugin will be trying to load it,
                    //which is in the database path
                    window.sqlitePlugin.getDatabasePath(localFileName, function (dbPath) {
                        // file does not exist
                        console.log('Database file [' + localFileName + '] does not exist, downloading file from ' + remoteFile);

                        msg.innerHTML = 'Downloading file (~14mbs)...';

                        ft = new FileTransfer();

                        ft.download(remoteFile, dbPath + localFileName, function (entry) {
                            console.log('download complete: ' + entry.fullPath);

                            that.buildMap();

                        }, function (error) {
                            console.log('error with download', error);
                        });
                    });

                });
            }
        });
    },
    buildMap: function () {
        var db = window.sqlitePlugin.openDatabase({name: localFileName, create: false});

        document.body.removeChild(msg);

        //set bounding box to limit user's ability to zoom out
        var southWest = new L.LatLng(40.4646, -112.1534);
        var northEast = new L.LatLng(40.8434, -111.7249);
        var restrictBounds = new L.LatLngBounds(southWest, northEast);
        var mapOptions = {
            maxBounds: restrictBounds,
            center: new L.LatLng(40.6681, -111.9364),
            zoom: 11,
            tms: true,
            maxZoom: 14,
            minZoom: 11
        };

        var map = new L.Map('map', mapOptions);

        var lyr = new L.TileLayer.MBTiles('', {maxZoom: 14, tms: true}, db);

        map.addLayer(lyr);
    }
};
