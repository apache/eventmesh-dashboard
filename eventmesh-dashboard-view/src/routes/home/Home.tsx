import React, { forwardRef } from "react";
import { Box, BoxProps } from "@mui/material";

interface HomeProps extends BoxProps {}

const Home = forwardRef<typeof Box, HomeProps>(({ ...props }, ref) => {
  return <Box ref={ref}>Home</Box>;
});

Home.displayName = "Home";
export default Home;