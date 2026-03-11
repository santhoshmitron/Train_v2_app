import express from 'express';
import {
  getAllManagegates,
  getManagegateById,
  createManagegate,
  updateManagegate,
  deleteManagegate,
  getManagegateStats,
  getBSLSGateIds
} from '../controllers/managegateController.js';

const router = express.Router();

router.get('/', getAllManagegates);
router.get('/stats', getManagegateStats);
router.get('/bs-ls-ids', getBSLSGateIds);
router.get('/:id', getManagegateById);
router.post('/', createManagegate);
router.put('/:id', updateManagegate);
router.delete('/:id', deleteManagegate);

export default router;
