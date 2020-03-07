package org.shadowice.flocke.andotp.Common.Network.Rest;

public class HttpCommon {

    /*HTTP Methods*/
    public final static String HTTPRequestMethodGET = "GET";
    public final static String HTTPRequestMethodHEAD = "HEAD";
    public final static String HTTPREquestMethodDELETE = "DELETE";
    public final static String HTTPRequestMethodPOST = "POST";
    public final static String HTTPRequestMethodPUT = "PUT";
    public final static String HTTPRequestMethodPATCH = "PATCH";

    /*HTTP Headers*/
    public final static String HTTPRequestHeaderNameContentType = "Content-Type";
    public final static String HTTPRequestHeaderNameContentLength = "Content-Length";
    public final static String HTTPRequestHeaderNameContentEncoding = "Content-Encoding";
    public final static String HTTPRequestHeaderNameAccept = "Accept";
    public final static String HTTPRequestHeaderApplicationType = "application-type";
    public final static String HTTPRequestHeaderAuthorization = "Authorization";
    public final static String HTTPRequestHeaderCacheControl = "Cache-Control";
    public final static String HTTPRequestHeaderCacheControlNoCache = "no-cache";

    public final static String HTTPURLRequestContentEncodingGZIP = "application/gzip";
    public final static String HTTPURLRequestContentTypeJSON = "application/json; charset=utf-8";
    public final static String HTTPURLRequestContentTypeXML = "application/xml; charset=utf-8";
    public final static String HTTPURLRequestContentTypeIMAGE = "image/*";
    public final static String HTTPURLRequestApplicationTypeREST = "REST";
    public final static String HTTPURLRequestContentTypeXWFORMURLENCODED = "application/x-www-form-urlencoded";
    public final static String HTTPURLRequestContentTypeBINARYOCTETSTREAM = "binary/octet-stream";
    public final static String HTTPURLRequestContentTypeAPPLICATIONOCTEMSTREAM = "application/octet-stream";
    public final static String HTTPURLRequestContentTypeMULTIPARTFORMDATA = "multipart/form-data";

    /*HTTP Status Codes*/
    public final static int HTTPStatusCodeOK = 200;
    public final static int HTTPStatusCodeCREATED = 201;
    public final static int HTTPStatusCodeACCEPTED = 202;
    public final static int HTTPStatusCodeNOCONTENT = 204;
    public final static int HTTPStatusCodeMULTIPLECHOICES = 300;
    public final static int HTTPStatusCodeFORBIDDEN = 403;
    public final static int HTTPStatusCodeNotFound = 404;
    public final static int HTTPStatusCodeMETHODNOTALLOWED = 405;
    public final static int HTTPSTatusCodeCONFLICT = 409;
    public final static int HTTPStatusCodeINTERNALERROR = 500;

    /*Mime Types*/
    public final static String HTTPMimeTypeTEXTPLAIN = "text/plain";
    public final static String HTTPMimeTypeTEXTHTML = "text/html";
    public final static String HTTPMimeTypeIMAGEJPEG = "image/jpeg";
    public final static String HTTPMimeTypeIMAGEPNG = "image/png";
    public final static String HTTPMimeTypeAUDIOMPEG = "audio/mpeg";
    public final static String HTTPMimeTypeAUDIOOGG = "audio/ogg";

}
