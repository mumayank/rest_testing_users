package com.tata.com.tata.users

import net.bytebuddy.implementation.bytecode.Throw
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users") // http://localhost:8080/users
class UsersApi {

    @Autowired
    lateinit var usersRepository: UserRepository

    @Autowired
    lateinit var bCryptPasswordEncoder: BCryptPasswordEncoder

    @GetMapping
    fun getUsers(): ArrayList<UserGetResponse> {
        val userGets = arrayListOf<UserGetResponse>()
        for (userEntitiy in usersRepository.findAll()) {
            val userGetRequest = UserGetResponse()
            BeanUtils.copyProperties(userEntitiy, userGetRequest)
            userGets.add(userGetRequest)
        }
        return userGets
    }

    @PostMapping
    @RequestMapping("/get")
    fun getUser(@RequestBody userGetRequest: UserGetRequest): UserGetResponse? {
        if (userGetRequest.email == null) {
            return null
        }

        val userEntity = usersRepository.findByEmail(userGetRequest.email)
        val userGetResponse = UserGetResponse()
        BeanUtils.copyProperties(userEntity, userGetResponse)
        return userGetResponse
    }

    @PostMapping
    fun createNewUser(@RequestBody userCreateNew: UserCreateNew): String {
        val userEntity = UserEntity()
        BeanUtils.copyProperties(userCreateNew, userEntity)
        userEntity.password = bCryptPasswordEncoder.encode(userEntity.password)
        usersRepository.save(userEntity)
        return "saved"
    }

    @PutMapping
    fun modifyUser(@RequestBody userModify: UserModify): String {
        if (userModify.email == null) {
            throw Exception("Invalid request. Please provide email id.")
        }

        val userEntity = usersRepository.findByEmail(userModify.email)

        if (userModify.oldPassword == null && userModify.newPassword == null) {
            // ignore
        }
        if (userModify.oldPassword != null && userModify.newPassword != null) {
            // user wants to change password
            if (bCryptPasswordEncoder.encode(userEntity.password) == userModify.oldPassword) {
                userEntity.password = userModify.newPassword
                userEntity.password = bCryptPasswordEncoder.encode(userEntity.password)
            } else {
                throw Exception("Incorrect current password.")
            }
        } else {
            throw Exception("Incorrect request. Please provide both old and new passwords to change password.")
        }
        if (userModify.name != null) {
            userEntity.name = userModify.name
        }

        usersRepository.save(userEntity)

        return "updated"
    }

    @DeleteMapping
    fun deleteUser(@RequestBody userDelete: UserDelete): String {
        if (userDelete.email == null) {
            return "Invalid request. Please provide email id."
        }

        val userEntity = usersRepository.findByEmail(userDelete.email)
        usersRepository.delete(userEntity)

        return "deleted"
    }

}