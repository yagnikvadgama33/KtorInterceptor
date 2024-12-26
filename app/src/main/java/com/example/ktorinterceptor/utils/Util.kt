package com.example.ktorinterceptor.utils

fun Map<String, String>.formUrlEncode(): String {
    return this.entries.joinToString("&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
}

fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")

fun String.replaceSpecialChar():String = this.replace("=",":").replace("&", ",")