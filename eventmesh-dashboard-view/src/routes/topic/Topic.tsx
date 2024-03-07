import React, { forwardRef } from "react";
import { Box, BoxProps } from "@mui/material";

interface TopicProps extends BoxProps {}

const Topic = forwardRef<typeof Box, TopicProps>(({ ...props }, ref) => {
  return <Box ref={ref}>Topic</Box>;
});

Topic.displayName = "Topic";
export default Topic;