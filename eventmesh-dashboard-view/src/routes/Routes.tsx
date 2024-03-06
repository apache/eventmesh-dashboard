import React from "react";
import { useRoutes, Navigate } from "react-router-dom";
import RootLayout from "./RootLayout";
import Home from "./home/Home";

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
      ],
    },
  ]);

};

export default AppRoutes;
