package com.learninghub.assessment.web;

/** Phase 1 stand-in for authentication: identity comes from headers. Phase 2 swaps this for JWT claims. */
public record CurrentUser(String username, String role) {
    public boolean isStudent() { return "student".equals(role); }
    public boolean isTeacher() { return "teacher".equals(role); }
    public boolean isAdmin()   { return "admin".equals(role); }
}
