import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.MODE === "development" ? "http://localhost:8080/api" : `${import.meta.env.VITE_BACK_END_URL}/api`,
    // baseURL: import.meta.env.VITE_BACK_END_URL,
    withCredentials: true,
});

export default api;