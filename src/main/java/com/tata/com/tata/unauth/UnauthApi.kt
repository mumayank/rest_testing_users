package com.tata.com.tata.unauth

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("signin") // http://localhost:8080/users
class UnauthApi {

    @PostMapping
    public fun signIn(@RequestBody userSignIn: UserSignIn): String {
        return "signin"
    }

}