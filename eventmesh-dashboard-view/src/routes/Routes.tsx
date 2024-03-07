import React from "react";
import { useRoutes, Navigate } from "react-router-dom";
import RootLayout from "./RootLayout";
import Home from "./home/Home";
import Topic from "./topic/Topic";

const AppRoutes = () => {
  return useRoutes([
    {
      path: "/",
      element: <RootLayout />,
      children: [
        {
          path: "*",
          element: <Navigate to="/home" replace />,
        },
        { path: "", element: <Navigate to="/home" /> },
        { path: "home", element: <Home /> },
        { path: "topic", element: <Topic /> },
      ],
    },
  ]);

};

export default AppRoutes;
