package com.example.textonly

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract

data class Contact(
    val name: String,
    val phone: String
)

class ContactsHelper(private val context: Context) {

    fun getContacts(): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val resolver: ContentResolver = context.contentResolver

        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Fără nume"
                val number = it.getString(numberIndex) ?: ""
                contactsList.add(Contact(name, number))
            }
        }

        return contactsList
    }
}
