# edutech

---

---

## Login `-->` Create Order `-->` Payment `-->` Treat Callback

### 1. LOG IN
* **Variable:**
  1. lms-token: store token returned from response after logging in
  2. device-id: store user's device logging in


* **Body payload:**
    ```
    {
        "email": "1banana@lms.com",
        "password": "123456",
        "deviceFingerPrint": "LAPTOP_26_08_17_02_32"
    }
  ```
  

* **Response structure:**
    ```
    {
        "code": 200,
        "message": "Login successfully",
        "data": {
            "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJkZXZpY2VGaW5nZXJQcmludCI6IkxBUFRPUF8yNl8wOF8xN18wMl8zMiIsInVzZXJJZCI6ImJmOWQ3ZDg2LWFiN2UtNDExNS1iYzdjLWIyMWUzN2E2ZDRiNyIsImF1dGhvcml0aWVzIjpbXSwic3ViIjoiMWJhbmFuYUBsbXMuY29tIiwiaWF0IjoxNzg5NDQ2MDUwLCJleHAiOjE3ODk1MzI0NTB9.4_8Hs4mZgYvJ-LUUruK4npTPo00UyHPmuYP7ac0c35kTzZaSenlhJ4rEoNfPHW3P",
            "userId": "bf9d7d86-ab7e-4115-bc7c-b21e37a6d4b7",
            "email": "1banana@lms.com",
            "permissions": []
        },
        "traceId": null
    }
  ```

RI8w5frC3fEGD+Cmr9g1FZta3bLmEkHGQULvCya83Uo=
RI8w5frC3fEGD+Cmr9g1FZta3bLmEkHGQULvCya83Uo=
