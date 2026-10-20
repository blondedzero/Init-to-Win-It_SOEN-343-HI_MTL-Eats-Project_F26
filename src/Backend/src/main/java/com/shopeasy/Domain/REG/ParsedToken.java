package com.shopeasy.Domain.REG;

import io.jsonwebtoken.Claims;

public record ParsedToken(Boolean isValid, Claims claims) {}
