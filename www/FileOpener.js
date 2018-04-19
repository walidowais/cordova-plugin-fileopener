var exec = require('cordova/exec');

exports.canOpenFile = function (fileURL, mimeType, success, error) {
    exec(success, error, "FileOpener", "canOpenFile", [fileURL, mimeType]);
};

exports.openFile = function (fileURL, mimeType, success, error) {
    exec(success, error, "FileOpener", "openFile", [fileURL, mimeType]);
};

