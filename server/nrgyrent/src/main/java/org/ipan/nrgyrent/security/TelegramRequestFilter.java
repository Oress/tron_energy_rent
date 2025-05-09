package org.ipan.nrgyrent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.bouncycastle.util.encoders.Hex;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;

/*
@Component
@AllArgsConstructor
public class TelegramRequestFilter extends OncePerRequestFilter {
    private static final String WEB_APP_DATA = "WebAppData";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final SecretKeySpec secretKeySpec = new SecretKeySpec(WEB_APP_DATA.getBytes(), HMAC_SHA_256);

    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String telegramInitDataHeader = request.getHeader("X-TGInitData");

        String queryStringUrlEnc = "user=%7B%22id%22%3A311958915%2C%22first_name%22%3A%22Ivan%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22bobugs%22%2C%22language_code%22%3A%22en%22%2C%22allows_write_to_pm%22%3Atrue%2C%22photo_url%22%3A%22https%3A%5C%2F%5C%2Ft.me%5C%2Fi%5C%2Fuserpic%5C%2F320%5C%2FVBq5iYj9S8p_Mn1IGv-1txRi5dVc-ipkzTLNmg-6Njw.svg%22%7D&chat_instance=2567317797361323372&chat_type=private&auth_date=1746477864&signature=JDEvZHJ3pIpk21_xdAZ-mjEKyBYYZ4p9TYA8SXWWT6NHNSbo3KFbma_0s2PtqMPqvE2iDmkc9d0KLEDIxkhjBA&hash=c2513923284016ce9712f2a45d553047ba68a2cd017aab557d25527b80e5c468";
        String queryStringUrlDecoded = URLDecoder.decode(queryStringUrlEnc, StandardCharsets.UTF_8);
        String[] queryStringParts = queryStringUrlDecoded.split("&");

        String dataCheckString = Arrays.stream(queryStringParts).sorted()
                .filter(part -> !part.startsWith("hash"))
                .collect(Collectors.joining("\n"));
        // Initialize HashMac
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(secretKeySpec);
            byte[] secretKeyBytes = mac.doFinal("7648926666:AAEcufT7SzNCOvyqbd--0gNHiZUAUasqVCA".getBytes());

            SecretKeySpec secretKeySpec1 = new SecretKeySpec(secretKeyBytes, HMAC_SHA_256);
            Mac mac1 = Mac.getInstance(HMAC_SHA_256);
            mac1.init(secretKeySpec1);
            String hashCompare = Hex.toHexString(mac1.doFinal(dataCheckString.getBytes()));

            if (!hashCompare.equals(telegramInitDataHeader)) {
                // Hash mismatch
                throw new RuntimeException("Hash mismatch");
            } else {
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        chain.doFilter(request, response);
    }
}*/
