package com.example.mylibrary.common.exceptions
open class DownloadManagerExceptions(message :String):Exception(message)

class NotEnoughSpaceException(message: String):DownloadManagerExceptions(message)

class FailedToDownloadException(message: String):DownloadManagerExceptions(message)