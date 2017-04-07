
function bytes_to_base64(arr){
  return btoa( bytes_to_string(arr) );
}


function base64_to_bytes(str){
  return string_to_bytes( atob( str ) );
}

function string_to_bytes ( str, utf8 ) {
  utf8 = !!utf8;

  var len = str.length,
    bytes = new Uint8Array( utf8 ? 4*len : len );

  for ( var i = 0, j = 0; i < len; i++ ) {
    var c = str.charCodeAt(i);

    if ( utf8 && 0xd800 <= c && c <= 0xdbff ) {
      if ( ++i >= len ) throw new Error( "Malformed string, low surrogate expected at position " + i );
      c = ( (c ^ 0xd800) << 10 ) | 0x10000 | ( str.charCodeAt(i) ^ 0xdc00 );
    }
    else if ( !utf8 && c >>> 8 ) {
      throw new Error("Wide characters are not allowed.");
    }

    if ( !utf8 || c <= 0x7f ) {
      bytes[j++] = c;
    }
    else if ( c <= 0x7ff ) {
      bytes[j++] = 0xc0 | (c >> 6);
      bytes[j++] = 0x80 | (c & 0x3f);
    }
    else if ( c <= 0xffff ) {
      bytes[j++] = 0xe0 | (c >> 12);
      bytes[j++] = 0x80 | (c >> 6 & 0x3f);
      bytes[j++] = 0x80 | (c & 0x3f);
    }
    else {
      bytes[j++] = 0xf0 | (c >> 18);
      bytes[j++] = 0x80 | (c >> 12 & 0x3f);
      bytes[j++] = 0x80 | (c >> 6 & 0x3f);
      bytes[j++] = 0x80 | (c & 0x3f);
    }
  }

  return bytes.subarray(0, j);
}

function bytes_to_string ( bytes, utf8 ) {
  utf8 = !!utf8;

  var len = bytes.length,
    chars = new Array(len);

  for ( var i = 0, j = 0; i < len; i++ ) {
    var b = bytes[i];
    if ( !utf8 || b < 128 ) {
      chars[j++] = b;
    }
    else if ( b >= 192 && b < 224 && i+1 < len ) {
      chars[j++] = ( (b & 0x1f) << 6 ) | (bytes[++i] & 0x3f);
    }
    else if ( b >= 224 && b < 240 && i+2 < len ) {
      chars[j++] = ( (b & 0xf) << 12 ) | ( (bytes[++i] & 0x3f) << 6 ) | (bytes[++i] & 0x3f);
    }
    else if ( b >= 240 && b < 248 && i+3 < len ) {
      var c = ( (b & 7) << 18 ) | ( (bytes[++i] & 0x3f) << 12 ) | ( (bytes[++i] & 0x3f) << 6 ) | (bytes[++i] & 0x3f);
      if ( c <= 0xffff ) {
        chars[j++] = c;
      }
      else {
        c ^= 0x10000;
        chars[j++] = 0xd800 | (c >> 10);
        chars[j++] = 0xdc00 | (c & 0x3ff);
      }
    }
    else {
      throw new Error("Malformed UTF8 character at byte offset " + i);
    }
  }

  var str = '',
    bs = 16384;
  for ( var i = 0; i < j; i += bs ) {
    str += String.fromCharCode.apply( String, chars.slice( i, i+bs <= j ? i+bs : j ) );
  }

  return str;
}

function getByteArray(id){
    var length = JSInterfaceByteArrayProvider.getLength(id);
    var array = new Uint8Array(length);
    for(var i = 0; i < length; i++){
       array[i] = JSInterfaceByteArrayProvider.getByte(id, i);
    }
    return array;
}

function stringifyToken(token){
	token.clientData.encKeyBytes = bytes_to_base64(token.clientData.encKeyBytes);
	token.clientData.saltBytes = bytes_to_base64(token.clientData.saltBytes);
	return JSON.stringify(token);
}

function parseTokenStr(str){
	var token = JSON.parse(str);
	token.clientData.encKeyBytes = base64_to_bytes(token.clientData.encKeyBytes);
	token.clientData.saltBytes = base64_to_bytes(token.clientData.saltBytes);
	return token;
}

function jsonTokenResponseFormatter(response){
    response.$token = stringifyToken(response.$token);
    return response;
}

function bin2string(array){
	var result = "";
	for(var i = 0; i < array.length; ++i){
		result+= (String.fromCharCode(array[i]));
	}
	return result;
}

function callFunction(json){
    var callData =  JSON.parse(json);
    var mainObj = callData.type == 2 ? mobileCmd : (callData.type == 1 ? cmd.api : this);

    for (i = 0; i < callData.extraArgs.length; i++) {
        var extraArg = callData.extraArgs[i];
        if(extraArg.type == "ByteArray"){
            callData.args.splice(extraArg.position, 0, bin2string(getByteArray(extraArg.id)));
        } else if (extraArg.type == "JSONToken"){
            callData.args.splice(extraArg.position, 0, parseTokenStr(extraArg.id));
        }
    }

    var obj = mainObj[callData.functionName];

    if (callData.type == 0){
        var response = obj.apply(obj, callData.args);
        response.password = "";
        JSInterfaceResponseHandler.onSuccess(JSON.stringify(response), callData.id)
    } else {
        obj.apply(obj, callData.args).then(function(succ) {
            if (callData.responseFormatter == "JSONToken"){
                succ = jsonTokenResponseFormatter(succ);
            }
            JSInterfaceResponseHandler.onSuccess(JSON.stringify(succ), callData.id);
        }, function(err) {
            JSInterfaceResponseHandler.onError(JSON.stringify(err), callData.id);
        });
     }
}