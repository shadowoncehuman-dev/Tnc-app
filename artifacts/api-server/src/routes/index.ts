import { Router, type IRouter } from "express";
import healthRouter from "./health";
import proxyRouter from "./proxy";
import botRouter from "./bot";
import mobileRouter from "./mobile";

const router: IRouter = Router();

router.use(healthRouter);
router.use("/bot", botRouter);
router.use(proxyRouter);
router.use("/mobile", mobileRouter);

export default router;
