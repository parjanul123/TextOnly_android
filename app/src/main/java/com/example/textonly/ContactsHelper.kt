package text.only.app

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract

class ContactsHelper(private val context: Context) {

    @SuppressLint("Range")
    fun getContacts(): List<Contact> {
        val list = mutableListOf<Contact>()
        val seen = HashSet<String>()

        val cursor = context.contentResolver.query(
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
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phone = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                // Eliminăm duplicatele bazate pe nume și telefon
                val key = "$name|$phone"
                if (seen.add(key)) {
                    list.add(Contact(name, phone))
                }
            }
        }

        return list
    }
}
