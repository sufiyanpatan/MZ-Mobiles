package mz.mobile.bussiness.dto;

//public record AuthResponseDTO(
//        String token,
//        String tokenType,
//        long expiresIn
//) {
//}


import java.util.List;

public record AuthResponseDTO(String token, String tokenType, long expiresIn,
                              String email, List<String> roles) {
}