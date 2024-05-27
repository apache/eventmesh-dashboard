import React, { forwardRef } from "react";
import { Box, BoxProps, Stack } from "@mui/material";
import { LoginOutlined } from "@mui/icons-material";

interface LoginProps extends BoxProps {}

const Login = forwardRef<typeof Box, LoginProps>(({ ...props }, ref) => {
  return <Box ref={ref}>
    <LoginOutlined />
    <Stack>
        <span>1</span>
        <span>2</span>
    </Stack>
  </Box>;
});

Login.displayName = "Login";
export default Login;