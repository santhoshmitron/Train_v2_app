import express from 'express';
import {
  getAllCustomers,
  getCustomerById,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  getCustomerStats,
  getSMCustomersWithGateInfo,
  getGMCustomersUnderSM,
  getGatesUnderSM,
  submitGateManOrStationMasterDetails,
  getGateManOrStationMasterDetails,
  getCustomerDetailsByUsername,
  upsertHelplinePhones,
  getHelplinePhones
} from '../controllers/customerController.js';

const router = express.Router();

// ✅ Specific routes FIRST
router.get('/get-gateman-stationmaster', getGateManOrStationMasterDetails);
router.get('/details-by-username', getCustomerDetailsByUsername);
router.post('/submit-gateman-stationmaster', submitGateManOrStationMasterDetails);
router.get('/stats', getCustomerStats);
router.get('/sm-with-gates', getSMCustomersWithGateInfo);
router.get('/gm-under-sm/:smUsername', getGMCustomersUnderSM);
router.get('/gates-under-sm/:smUsername', getGatesUnderSM);
router.post('/helpline', upsertHelplinePhones);
router.get('/helpline', getHelplinePhones);

// ✅ Then general routes LAST
router.get('/', getAllCustomers);
router.get('/:id', getCustomerById);
router.post('/', createCustomer);
router.put('/:id', updateCustomer);
router.delete('/:id', deleteCustomer);

export default router;
