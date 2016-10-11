/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.supplychain.service.invoice.generator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.axelor.apps.account.db.AnalyticDistributionLine;
import com.axelor.apps.account.db.BudgetDistribution;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.AnalyticDistributionLineRepository;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.InvoiceToolService;
import com.axelor.apps.account.service.invoice.generator.InvoiceLineGenerator;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.administration.GeneralService;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.supplychain.db.Subscription;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;

/**
 * Classe de création de ligne de facture abstraite.
 *
 */
public abstract class InvoiceLineGeneratorSupplyChain extends InvoiceLineGenerator {

	@Inject
	protected InvoiceLineService invoiceLineService;
	
	@Inject
	protected UnitConversionService unitConversionService;
	
	protected SaleOrderLine saleOrderLine;
	protected PurchaseOrderLine purchaseOrderLine;
	protected StockMoveLine stockMoveLine;
	protected Subscription subscription;
	
	protected InvoiceLineGeneratorSupplyChain( Invoice invoice, Product product, String productName, String description, BigDecimal qty,
			Unit unit, int sequence, boolean isTaxInvoice, SaleOrderLine saleOrderLine, PurchaseOrderLine purchaseOrderLine, StockMoveLine stockMoveLine) throws AxelorException {

		super(invoice, product, productName, description, qty, unit, sequence, isTaxInvoice);

		this.saleOrderLine = saleOrderLine;
		this.purchaseOrderLine = purchaseOrderLine;
		this.stockMoveLine = stockMoveLine;
		
		if(saleOrderLine != null)  {
			this.discountAmount = saleOrderLine.getDiscountAmount();
			this.price = saleOrderLine.getPrice();
			this.priceDiscounted = saleOrderLine.getPriceDiscounted();
			this.taxLine = saleOrderLine.getTaxLine();
			this.discountTypeSelect = saleOrderLine.getDiscountTypeSelect();
			this.isTitleLine = saleOrderLine.getIsTitleLine();
		} else if (purchaseOrderLine != null){
			this.isTitleLine = purchaseOrderLine.getIsTitleLine();
			this.purchaseOrderLine = purchaseOrderLine;
			this.discountAmount = purchaseOrderLine.getDiscountAmount();
			this.price = purchaseOrderLine.getPrice();
			this.priceDiscounted = purchaseOrderLine.getPriceDiscounted();
			this.taxLine = purchaseOrderLine.getTaxLine();
			this.discountTypeSelect = purchaseOrderLine.getDiscountTypeSelect();
		}
		else if(stockMoveLine != null)  {
			this.priceDiscounted = stockMoveLine.getUnitPriceUntaxed();
			Unit saleOrPurchaseUnit = this.getSaleOrPurchaseUnit();
			
			if(saleOrPurchaseUnit != null && this.unit != null && !this.unit.equals(saleOrPurchaseUnit)){
				this.qty = Beans.get(UnitConversionService.class).convertWithProduct(this.unit, saleOrPurchaseUnit, qty, stockMoveLine.getProduct());
				this.priceDiscounted = Beans.get(UnitConversionService.class).convertWithProduct(this.unit, saleOrPurchaseUnit, this.priceDiscounted, product);
				this.unit = saleOrPurchaseUnit;
			}

		}
		
    }
	

	protected InvoiceLineGeneratorSupplyChain( Invoice invoice, Product product, String productName, String description, BigDecimal qty,
			Unit unit, int sequence, boolean isTaxInvoice, SaleOrderLine saleOrderLine, PurchaseOrderLine purchaseOrderLine, 
			StockMoveLine stockMoveLine, Subscription subscription) throws AxelorException {

		this(invoice, product, productName, description, qty, unit, sequence, isTaxInvoice, saleOrderLine, purchaseOrderLine, stockMoveLine);

		this.subscription = subscription;
    }
	

	/**
	 * @return
	 * @throws AxelorException
	 */
	@Override
	protected InvoiceLine createInvoiceLine() throws AxelorException  {

		InvoiceLine invoiceLine = super.createInvoiceLine();
		
		// Update for subscription SaleOrderLine
		if (saleOrderLine != null && product != null && ProductRepository.PRODUCT_TYPE_SUBSCRIPTABLE.equals(product.getProductTypeSelect())
				&& saleOrderLine.getSubscriptionList() != null && !saleOrderLine.getSubscriptionList().isEmpty())  {
			BigDecimal subscriptionListSize = new BigDecimal(saleOrderLine.getSubscriptionList().size());
			this.exTaxTotal = this.exTaxTotal.divide(subscriptionListSize, 2, RoundingMode.HALF_EVEN);
			this.inTaxTotal = this.inTaxTotal.divide(subscriptionListSize, 2, RoundingMode.HALF_EVEN);
		}

		this.assignOriginElements(invoiceLine);
		
		if (saleOrderLine != null)  {
			
			this.copyAnalyticDistributionLines(saleOrderLine.getAnalyticDistributionLineList(), invoiceLine);

			
		} else if (purchaseOrderLine != null)  {
			
			this.copyAnalyticDistributionLines(purchaseOrderLine.getAnalyticDistributionLineList(), invoiceLine);
					
			this.copyBudgetDistributionList(purchaseOrderLine.getBudgetDistributionList(), invoiceLine);
			
			invoiceLine.setBudget(purchaseOrderLine.getBudget());
		}
		
		else if(stockMoveLine != null)  {
			
			this.price = invoiceLineService.getUnitPrice(invoice, invoiceLine, taxLine, InvoiceToolService.isPurchase(invoice));
			this.price = Beans.get(UnitConversionService.class).convertWithProduct(stockMoveLine.getUnit(), this.unit, this.price, product);

		}
		
		return invoiceLine;

	}
	
	
	public void assignOriginElements(InvoiceLine invoiceLine) throws AxelorException  {
		
		if (!Beans.get(GeneralService.class).getGeneral().getManageInvoicedAmountByLine())  {  return;  }
		
		StockMove stockMove = null;
		if(stockMoveLine != null)  {  stockMove = stockMoveLine.getStockMove();  }
		
		if(stockMove != null)  { 
			if(InvoiceToolService.isPurchase(invoice))  {
				invoiceLine.setIncomingStockMove(stockMove);
			}
			else  {
				invoiceLine.setOutgoingStockMove(stockMove);
			}
		}
		
		if(saleOrderLine != null)  {
			invoiceLine.setSaleOrderLine(saleOrderLine);
		}
		if(purchaseOrderLine != null)  {
			invoiceLine.setPurchaseOrderLine(purchaseOrderLine);
		}
		
	}
	
	
	public void copyAnalyticDistributionLines(List<AnalyticDistributionLine> originalAnalyticDistributionLineList, InvoiceLine invoiceLine)  {
		
		if(originalAnalyticDistributionLineList == null)  {  return;  }
 
		for (AnalyticDistributionLine analyticDistributionLineIt : originalAnalyticDistributionLineList) {
			invoiceLine.addAnalyticDistributionLineListItem(Beans.get(AnalyticDistributionLineRepository.class).copy(analyticDistributionLineIt, false));
		}
				
	}
	
	public void copyBudgetDistributionList(List<BudgetDistribution> originalBudgetDistributionList, InvoiceLine invoiceLine)  {
		
		if(originalBudgetDistributionList == null)  {  return ;  }
 
		for (BudgetDistribution budgetDistributionIt : originalBudgetDistributionList) {
			BudgetDistribution budgetDistribution = new BudgetDistribution();
			budgetDistribution.setBudget(budgetDistributionIt.getBudget());
			budgetDistribution.setAmount(budgetDistributionIt.getAmount());
			invoiceLine.addBudgetDistributionListItem(budgetDistribution);
		}
	}
	
	public Unit getSaleOrPurchaseUnit() throws AxelorException  {
		
		if(!InvoiceToolService.isPurchase(invoice)){
			return product.getSalesUnit();
		}
		else{
			return product.getPurchasesUnit();
		}
	}

}