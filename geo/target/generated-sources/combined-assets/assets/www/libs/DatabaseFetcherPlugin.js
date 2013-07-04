/**
 * Kaidad GEO
 */
;
(function () {
    var root,
        ProgressEvent = cordova.require('cordova/plugin/ProgressEvent'),
        idCounter = 0;

    root = this;
    function newProgressEvent(result) {
        var pe = new ProgressEvent();
        pe.lengthComputable = result.lengthComputable;
        pe.loaded = result.loaded;
        pe.total = result.total;
        return pe;
    }

    function getBasicAuthHeader(urlString) {
        var header =  null;

        if (window.btoa) {
            // parse the url using the Location object
            var url = document.createElement('a');
            url.href = urlString;

            var credentials = null;
            var protocol = url.protocol + "//";
            var origin = protocol + url.host;

            // check whether there are the username:password credentials in the url
            if (url.href.indexOf(origin) !== 0) { // credentials found
                var atIndex = url.href.indexOf("@");
                credentials = url.href.substring(protocol.length, atIndex);
            }

            if (credentials) {
                var authHeader = "Authorization";
                var authHeaderValue = "Basic " + window.btoa(credentials);

                header = {
                    name : authHeader,
                    value : authHeaderValue
                };
            }
        }

        return header;
    }

    /**
     * DatabaseFetcher fetches remote SQLite database files if they
     * do not already exist. This code borrows on the FileTransfer
     * object from Cordova.
     *
     * @constructor
     */
    var DatabaseFetcherPlugin = function () {
        this._id = ++idCounter;
        this.onprogress = null; // optional callback
    };

    /**
     * Downloads a file form a given URL and saves it to the specified directory.
     * @param dbName {String}          The name of the Database
     * @param source {String}          The name of the Database
     * @param successCallback (Function}  Callback to be invoked when upload has completed
     * @param errorCallback {Function}    Callback to be invoked upon error
     * @param trustAllHosts {Boolean} Optional trust all hosts (e.g. for self-signed certs), defaults to false
     * @param options {FileDownloadOptions} Optional parameters such as headers
     */
    DatabaseFetcherPlugin.prototype.fetch = function (dbName, source, successCallback, errorCallback, trustAllHosts, options) {
        var self = this;

        var basicAuthHeader = getBasicAuthHeader(source);
        if (basicAuthHeader) {
            options = options || {};
            options.headers = options.headers || {};
            options.headers[basicAuthHeader.name] = basicAuthHeader.value;
        }

        var headers = null;
        if (options) {
            headers = options.headers || null;
        }

        var win = function (result) {
            if (typeof result.lengthComputable != "undefined") {
                if (self.onprogress) {
                    return self.onprogress(newProgressEvent(result));
                }
            } else if (successCallback) {
                var entry = null;
                if (result.isFile) {
                    entry = new (require('cordova/plugin/FileEntry'))();
                }
                entry.isDirectory = result.isDirectory;
                entry.isFile = result.isFile;
                entry.name = result.name;
                entry.fullPath = result.fullPath;
                successCallback(entry);
            }
        };

        var fail = errorCallback && function (e) {
            var error = new FileTransferError(e.code, e.source, e.target, e.http_status, e.body);
            errorCallback(error);
        };

        cordova.exec(win, fail, 'DatabaseFetcherPlugin', 'fetch', [dbName, source, trustAllHosts, this._id, headers]);
    };

    return root.databaseFetcherPlugin = {
        fetch: new DatabaseFetcherPlugin().fetch
    };
})();