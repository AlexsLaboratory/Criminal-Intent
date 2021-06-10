package com.bignerdranch.andriod.criminalintent

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun isEnabledPermissions(context: Context, permissionArray: Array<String>): Boolean {
  permissionArray.forEach {
    if (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED) {
      return false
    }
  }
  return true
}