package chat.sphinx.dto_common

// TODO: Write extension functions.
//  Contact and Chat Dto fields 'created_at' and 'updated_at'
//  are formatted like: 2021-02-26T10:48:20.025Z
//  whereas the Contact's 'last_active' field's format
//  is: 2021-03-12 14:26:18.000 +00:00

inline class DateTime(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "DateTime cannot be empty"
        }
    }
}
